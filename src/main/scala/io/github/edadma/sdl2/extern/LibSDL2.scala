package io.github.edadma.sdl2.extern

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Raw `@extern` bindings to SDL2 — the only place Scala Native FFI types
  * appear. Consumers never import this; they use the pure-Scala layer in the
  * `io.github.edadma.sdl2` package, which wraps these in `AnyVal` types and
  * Scala-native values. `@link("SDL2")` makes the native linker pull in libSDL2.
  */
@link("SDL2")
@extern
object LibSDL2:
  // Opaque handles — pointers the high-level layer wraps in AnyVal types.
  type SDL_Window   = Ptr[Byte]
  type SDL_Renderer = Ptr[Byte]
  type SDL_Texture  = Ptr[Byte]
  type SDL_Surface  = Ptr[Byte]

  // SDL_Event is a ~56-byte union; the high-level layer keeps one buffer and
  // reads fields by their (ABI-stable, 64-bit) offsets.
  type SDL_Event = Ptr[Byte]

  // int (*)(void *userdata, SDL_Event *event) — used by event filters/watches.
  type SDL_EventFilter = CFuncPtr2[Ptr[Byte], Ptr[Byte], CInt]

  def SDL_SetMainReady(): Unit                         = extern
  def SDL_Init(flags: UInt): CInt                      = extern
  def SDL_Quit(): Unit                                 = extern
  def SDL_GetError(): CString                          = extern
  def SDL_Delay(ms: UInt): Unit                        = extern
  def SDL_SetHint(name: CString, value: CString): CInt = extern

  def SDL_CreateWindow(title: CString, x: CInt, y: CInt, w: CInt, h: CInt, flags: UInt): SDL_Window = extern
  def SDL_DestroyWindow(window: SDL_Window): Unit                                                    = extern
  def SDL_GetWindowSize(window: SDL_Window, w: Ptr[CInt], h: Ptr[CInt]): Unit                        = extern
  def SDL_GetWindowPixelFormat(window: SDL_Window): UInt                                             = extern

  def SDL_CreateRenderer(window: SDL_Window, index: CInt, flags: UInt): SDL_Renderer            = extern
  def SDL_DestroyRenderer(renderer: SDL_Renderer): Unit                                         = extern
  def SDL_SetRenderDrawColor(renderer: SDL_Renderer, r: UByte, g: UByte, b: UByte, a: UByte): CInt = extern
  def SDL_SetRenderDrawBlendMode(renderer: SDL_Renderer, blendMode: CInt): CInt                 = extern
  def SDL_RenderClear(renderer: SDL_Renderer): CInt                                             = extern
  def SDL_RenderDrawPoint(renderer: SDL_Renderer, x: CInt, y: CInt): CInt                       = extern
  def SDL_RenderDrawLine(renderer: SDL_Renderer, x1: CInt, y1: CInt, x2: CInt, y2: CInt): CInt  = extern
  def SDL_RenderFillRect(renderer: SDL_Renderer, rect: Ptr[CInt]): CInt                         = extern
  def SDL_RenderCopy(renderer: SDL_Renderer, texture: SDL_Texture, src: Ptr[Byte], dst: Ptr[Byte]): CInt = extern
  def SDL_RenderPresent(renderer: SDL_Renderer): Unit                                           = extern
  def SDL_SetRenderTarget(renderer: SDL_Renderer, texture: SDL_Texture): CInt                   = extern

  def SDL_CreateTexture(renderer: SDL_Renderer, format: UInt, access: CInt, w: CInt, h: CInt): SDL_Texture = extern
  def SDL_CreateTextureFromSurface(renderer: SDL_Renderer, surface: SDL_Surface): SDL_Texture   = extern
  def SDL_DestroyTexture(texture: SDL_Texture): Unit                                            = extern
  def SDL_SetTextureScaleMode(texture: SDL_Texture, scaleMode: CInt): CInt                      = extern
  def SDL_QueryTexture(texture: SDL_Texture, format: Ptr[UInt], access: Ptr[CInt], w: Ptr[CInt], h: Ptr[CInt]): CInt = extern
  def SDL_FreeSurface(surface: SDL_Surface): Unit                                               = extern

  def SDL_PollEvent(event: SDL_Event): CInt                  = extern
  def SDL_GetKeyboardState(numkeys: Ptr[CInt]): Ptr[UByte]   = extern
  def SDL_GetMouseState(x: Ptr[CInt], y: Ptr[CInt]): UInt    = extern
  def SDL_AddEventWatch(filter: SDL_EventFilter, userdata: Ptr[Byte]): Unit = extern
  def SDL_DelEventWatch(filter: SDL_EventFilter, userdata: Ptr[Byte]): Unit = extern
