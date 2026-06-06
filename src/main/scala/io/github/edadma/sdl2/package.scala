package io.github.edadma

import scala.collection.mutable
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.stdlib

/** Pure-Scala SDL2 layer. This is the only package consumers import — it speaks
  * in `Int`, `Boolean`, `String`, `Color`, and `AnyVal`-wrapped handles, never
  * in Scala Native FFI types. Native pointers are wrapped in `AnyVal` value
  * classes (`Window`, `Renderer`, `Texture`), so the abstraction is free.
  *
  * SDL is Native-only; this artifact has no JVM/JS build.
  */
package object sdl2:

  import extern.{LibSDL2 => sdl, LibSDL2Gfx => gfx}

  // ---- subsystem init flags (SDL_INIT_*) ----
  val INIT_TIMER    = 0x00000001
  val INIT_AUDIO    = 0x00000010
  val INIT_VIDEO    = 0x00000020
  val INIT_EVENTS   = 0x00004000

  // ---- window position / flags ----
  val WINDOWPOS_CENTERED = 0x2fff0000
  val WINDOW_FULLSCREEN  = 0x00000001
  val WINDOW_OPENGL      = 0x00000002
  val WINDOW_SHOWN       = 0x00000004
  val WINDOW_HIDDEN      = 0x00000008
  val WINDOW_BORDERLESS  = 0x00000010
  val WINDOW_RESIZABLE   = 0x00000020
  val WINDOW_ALLOW_HIGHDPI = 0x00002000

  // ---- renderer flags (SDL_RENDERER_*) ----
  val RENDERER_SOFTWARE      = 0x00000001
  val RENDERER_ACCELERATED   = 0x00000002
  val RENDERER_PRESENTVSYNC  = 0x00000004
  val RENDERER_TARGETTEXTURE = 0x00000008

  // ---- texture access / scale mode / blend mode ----
  val TEXTUREACCESS_STATIC    = 0
  val TEXTUREACCESS_STREAMING = 1
  val TEXTUREACCESS_TARGET    = 2
  val SCALEMODE_NEAREST       = 0
  val SCALEMODE_LINEAR        = 1
  val SCALEMODE_BEST          = 2
  val BLENDMODE_NONE          = 0
  val BLENDMODE_BLEND         = 1
  val BLENDMODE_ADD           = 2
  val BLENDMODE_MOD           = 4

  // ---- event types (SDL_*) ----
  val QUIT            = 0x100
  val WINDOWEVENT     = 0x200
  val KEYDOWN         = 0x300
  val KEYUP           = 0x301
  val MOUSEMOTION     = 0x400
  val MOUSEBUTTONDOWN = 0x401
  val MOUSEBUTTONUP   = 0x402
  val MOUSEWHEEL      = 0x403

  // ---- mouse button masks (SDL_GetMouseState) ----
  val BUTTON_LMASK = 1
  val BUTTON_MMASK = 2
  val BUTTON_RMASK = 4

  // ---- hint names ----
  val HINT_RENDER_SCALE_QUALITY = "SDL_RENDER_SCALE_QUALITY"

  /** An RGBA colour, 0–255 per channel. Not a pointer, so a plain case class. */
  final case class Color(r: Int, g: Int, b: Int, a: Int = 255)

  object Color:
    /** From a packed `0xRRGGBB` value, fully opaque. */
    def fromRGB(rgb: Int): Color = Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff)
    val Black = Color(0, 0, 0)
    val White = Color(255, 255, 255)

    /** Linear interpolation from `a` (t=0) to `b` (t=1), clamped — handy for
      * fades (e.g. trails blending toward the background).
      */
    def blend(a: Color, b: Color, t: Double): Color =
      val u = if t < 0.0 then 0.0 else if t > 1.0 then 1.0 else t
      Color(
        (a.r + (b.r - a.r) * u).toInt,
        (a.g + (b.g - a.g) * u).toInt,
        (a.b + (b.b - a.b) * u).toInt,
        (a.a + (b.a - a.a) * u).toInt,
      )

  // ---- top-level lifecycle ----

  /** Tell SDL we provide our own `main` (required when not using SDL's main shim). */
  def setMainReady(): Unit = sdl.SDL_SetMainReady()

  /** Initialise SDL subsystems; `true` on success. */
  def init(flags: Int = INIT_VIDEO): Boolean = sdl.SDL_Init(flags.toUInt) == 0

  def quit(): Unit = sdl.SDL_Quit()

  /** The last SDL error message. */
  def error: String = fromCString(sdl.SDL_GetError())

  def delay(ms: Int): Unit = sdl.SDL_Delay(ms.toUInt)

  def setHint(name: String, value: String): Boolean =
    Zone(sdl.SDL_SetHint(toCString(name), toCString(value)) == 1)

  /** Create a window. SDL copies the title, so it is freed when the zone closes. */
  def createWindow(
      title: String,
      width: Int,
      height: Int,
      x: Int = WINDOWPOS_CENTERED,
      y: Int = WINDOWPOS_CENTERED,
      flags: Int = WINDOW_SHOWN,
  ): Window =
    Zone(new Window(sdl.SDL_CreateWindow(toCString(title), x, y, width, height, flags.toUInt)))

  // ---- handle wrappers (AnyVal — pointers, zero-cost) ----

  implicit class Window(val ptr: sdl.SDL_Window) extends AnyVal:
    def isNull: Boolean = ptr == null
    def createRenderer(
        flags: Int = RENDERER_ACCELERATED | RENDERER_PRESENTVSYNC,
        index: Int = -1,
    ): Renderer = new Renderer(sdl.SDL_CreateRenderer(ptr, index, flags.toUInt))
    def pixelFormat: Int = sdl.SDL_GetWindowPixelFormat(ptr).toInt
    def size: (Int, Int) =
      val w = stackalloc[CInt]()
      val h = stackalloc[CInt]()
      sdl.SDL_GetWindowSize(ptr, w, h)
      (!w, !h)
    def destroy(): Unit = sdl.SDL_DestroyWindow(ptr)

  implicit class Renderer(val ptr: sdl.SDL_Renderer) extends AnyVal:
    def isNull: Boolean = ptr == null
    def setDrawColor(r: Int, g: Int, b: Int, a: Int = 255): Unit =
      sdl.SDL_SetRenderDrawColor(ptr, r.toUByte, g.toUByte, b.toUByte, a.toUByte)
    def setDrawColor(c: Color): Unit = setDrawColor(c.r, c.g, c.b, c.a)
    def setBlendMode(mode: Int): Unit = sdl.SDL_SetRenderDrawBlendMode(ptr, mode)
    def clear(): Unit                 = sdl.SDL_RenderClear(ptr)
    def clear(c: Color): Unit         = { setDrawColor(c); sdl.SDL_RenderClear(ptr) }
    def drawPoint(x: Int, y: Int): Unit = sdl.SDL_RenderDrawPoint(ptr, x, y)
    def drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Unit = sdl.SDL_RenderDrawLine(ptr, x1, y1, x2, y2)
    def fillRect(x: Int, y: Int, w: Int, h: Int): Unit =
      val r = stackalloc[CInt](4)
      r(0) = x; r(1) = y; r(2) = w; r(3) = h
      sdl.SDL_RenderFillRect(ptr, r)
    def present(): Unit                 = sdl.SDL_RenderPresent(ptr)
    def setTarget(t: Texture): Unit     = sdl.SDL_SetRenderTarget(ptr, t.ptr)
    def resetTarget(): Unit             = sdl.SDL_SetRenderTarget(ptr, null)
    /** Blit a whole texture across the entire render target. */
    def copy(t: Texture): Unit = sdl.SDL_RenderCopy(ptr, t.ptr, null, null)
    /** Blit a texture into a destination rectangle, in target pixels. */
    def copy(t: Texture, x: Int, y: Int, w: Int, h: Int): Unit =
      val dst = stackalloc[CInt](4)
      dst(0) = x; dst(1) = y; dst(2) = w; dst(3) = h
      sdl.SDL_RenderCopy(ptr, t.ptr, null, dst.asInstanceOf[Ptr[Byte]])
    /** Blit a texture at `(x, y)` using its own pixel size. */
    def copy(t: Texture, x: Int, y: Int): Unit =
      val (w, h) = t.size
      copy(t, x, y, w, h)
    def createTexture(format: Int, access: Int, w: Int, h: Int): Texture =
      new Texture(sdl.SDL_CreateTexture(ptr, format.toUInt, access, w, h))
    /** Upload a CPU surface (e.g. from SDL2_ttf) to a GPU texture. */
    def createTextureFromSurface(s: Surface): Texture =
      new Texture(sdl.SDL_CreateTextureFromSurface(ptr, s.ptr))
    def destroy(): Unit = sdl.SDL_DestroyRenderer(ptr)

    // Antialiased / filled primitives via SDL2_gfx.
    def aaLine(x1: Int, y1: Int, x2: Int, y2: Int, c: Color): Unit =
      gfx.aalineRGBA(ptr, x1.toShort, y1.toShort, x2.toShort, y2.toShort, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)
    def line(x1: Int, y1: Int, x2: Int, y2: Int, c: Color): Unit =
      gfx.lineRGBA(ptr, x1.toShort, y1.toShort, x2.toShort, y2.toShort, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)
    def thickLine(x1: Int, y1: Int, x2: Int, y2: Int, width: Int, c: Color): Unit =
      gfx.thickLineRGBA(ptr, x1.toShort, y1.toShort, x2.toShort, y2.toShort, width.toUByte, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)
    def drawCircle(x: Int, y: Int, radius: Int, c: Color): Unit =
      gfx.aacircleRGBA(ptr, x.toShort, y.toShort, radius.toShort, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)
    /** Filled, antialiased circle. */
    def fillCircle(x: Int, y: Int, radius: Int, c: Color): Unit =
      gfx.filledCircleRGBA(ptr, x.toShort, y.toShort, radius.toShort, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)
      gfx.aacircleRGBA(ptr, x.toShort, y.toShort, radius.toShort, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)
    def fillBox(x1: Int, y1: Int, x2: Int, y2: Int, c: Color): Unit =
      gfx.boxRGBA(ptr, x1.toShort, y1.toShort, x2.toShort, y2.toShort, c.r.toUByte, c.g.toUByte, c.b.toUByte, c.a.toUByte)

  implicit class Texture(val ptr: sdl.SDL_Texture) extends AnyVal:
    def isNull: Boolean               = ptr == null
    def setScaleMode(mode: Int): Unit = sdl.SDL_SetTextureScaleMode(ptr, mode)
    /** The texture's `(width, height)` in pixels. */
    def size: (Int, Int) =
      val w = stackalloc[CInt]()
      val h = stackalloc[CInt]()
      sdl.SDL_QueryTexture(ptr, null, null, w, h)
      (!w, !h)
    def destroy(): Unit = sdl.SDL_DestroyTexture(ptr)

  /** A CPU-side pixel buffer — produced by SDL2_ttf text rendering, then
    * uploaded to a [[Texture]] via [[Renderer.createTextureFromSurface]] and
    * freed. `width`/`height` read the SDL_Surface struct (64-bit layout).
    */
  implicit class Surface(val ptr: sdl.SDL_Surface) extends AnyVal:
    def isNull: Boolean  = ptr == null
    def width: Int       = !((ptr + 16).asInstanceOf[Ptr[CInt]])
    def height: Int      = !((ptr + 20).asInstanceOf[Ptr[CInt]])
    def free(): Unit     = sdl.SDL_FreeSurface(ptr)

  // ---- events ----

  // One reusable heap buffer for the SDL_Event union (kept for the process
  // lifetime). Each poll overwrites it, which is fine for the usual
  // poll-then-handle loop.
  private val eventBuf: Ptr[Byte] = stdlib.malloc(128.toUSize)

  /** Pull the next pending event, or `None` if the queue is empty. */
  def pollEvent(): Option[Event] =
    if sdl.SDL_PollEvent(eventBuf) != 0 then Some(new Event(eventBuf)) else None

  /** A view over the current SDL_Event buffer. Field accessors are only
    * meaningful for the matching [[kind]] (the underlying struct is a union).
    * Offsets are the stable SDL2 64-bit ABI layout.
    */
  implicit class Event(val ptr: Ptr[Byte]) extends AnyVal:
    private def i32(off: Int): Int = !((ptr + off).asInstanceOf[Ptr[Int]])
    private def u8(off: Int): Int  = (!(ptr + off)).toInt & 0xff

    def kind: Int = (!ptr.asInstanceOf[Ptr[UInt]]).toInt

    /** Keyboard events: the physical key (an SDL scancode) and key-repeat flag. */
    def keyScancode: Int     = i32(16)
    def keyRepeat: Boolean   = u8(13) != 0
    /** Mouse motion/button events: cursor position. */
    def mouseX: Int = i32(20)
    def mouseY: Int = i32(24)
    /** Mouse button events: which button (1=left, 2=middle, 3=right). */
    def mouseButton: Int = u8(16)
    /** Mouse wheel events: scroll amounts (positive y = away from the user). */
    def wheelX: Int = i32(16)
    def wheelY: Int = i32(20)

  // ---- event watches: the libuv-style callback map pattern ----
  //
  // SDL invokes a C function pointer for each event during pumping. We register
  // ONE static trampoline with SDL and keep the Scala watchers in a map, so
  // consumers pass ordinary closures. Watches fire on the thread that pumps
  // events (the main thread here), so this stays clear of Scala Native's GC.

  type EventWatch = Event => Unit

  private val eventWatches    = mutable.HashMap[Int, EventWatch]()
  private var nextWatchId     = 0
  private var watchRegistered = false

  private val eventWatchTrampoline: sdl.SDL_EventFilter =
    (_: Ptr[Byte], event: Ptr[Byte]) =>
      val e = new Event(event)
      eventWatches.valuesIterator.foreach(_(e))
      0

  /** Register a watcher invoked for every event as it is pumped. Returns an id
    * for [[removeEventWatch]].
    */
  def addEventWatch(watch: EventWatch): Int =
    if !watchRegistered then
      sdl.SDL_AddEventWatch(eventWatchTrampoline, null)
      watchRegistered = true
    val id = nextWatchId
    nextWatchId += 1
    eventWatches(id) = watch
    id

  def removeEventWatch(id: Int): Unit =
    eventWatches -= id
    if eventWatches.isEmpty && watchRegistered then
      sdl.SDL_DelEventWatch(eventWatchTrampoline, null)
      watchRegistered = false

  // ---- keyboard / mouse polling ----

  object Keyboard:
    /** A snapshot of held keys, indexed by [[Scancode]]. */
    def state: KeyboardState = new KeyboardState(sdl.SDL_GetKeyboardState(null))

  implicit class KeyboardState(val ptr: Ptr[UByte]) extends AnyVal:
    def apply(scancode: Int): Boolean = ptr(scancode).toInt != 0

  final case class MouseState(buttons: Int, x: Int, y: Int):
    def left: Boolean   = (buttons & BUTTON_LMASK) != 0
    def middle: Boolean = (buttons & BUTTON_MMASK) != 0
    def right: Boolean  = (buttons & BUTTON_RMASK) != 0

  object Mouse:
    def state: MouseState =
      val x    = stackalloc[CInt]()
      val y    = stackalloc[CInt]()
      val mask = sdl.SDL_GetMouseState(x, y)
      MouseState(mask.toInt, !x, !y)

  /** A useful subset of SDL physical scancodes (USB HID usage IDs), which is
    * what [[KeyboardState.apply]] is indexed by.
    */
  object Scancode:
    val A = 4; val B = 5; val C = 6; val D = 7; val E = 8; val F = 9; val G = 10
    val H = 11; val I = 12; val J = 13; val K = 14; val L = 15; val M = 16; val N = 17
    val O = 18; val P = 19; val Q = 20; val R = 21; val S = 22; val T = 23; val U = 24
    val V = 25; val W = 26; val X = 27; val Y = 28; val Z = 29
    val Num1 = 30; val Num2 = 31; val Num3 = 32; val Num4 = 33; val Num5 = 34
    val Num6 = 35; val Num7 = 36; val Num8 = 37; val Num9 = 38; val Num0 = 39
    val Return = 40; val Escape = 41; val Backspace = 42; val Tab = 43; val Space = 44
    val Minus = 45; val Equals = 46; val LeftBracket = 47; val RightBracket = 48
    val Right = 79; val Left = 80; val Down = 81; val Up = 82
