# sdl2

![Maven Central](https://img.shields.io/maven-central/v/io.github.edadma/sdl2_native0.5_3)
[![Last Commit](https://img.shields.io/github/last-commit/edadma/sdl2)](https://github.com/edadma/sdl2/commits)
![GitHub](https://img.shields.io/github/license/edadma/sdl2)
![Scala Version](https://img.shields.io/badge/Scala-3.8.4-blue.svg)
![Scala Native Version](https://img.shields.io/badge/Scala_Native-0.5.12-blue.svg)

Scala Native bindings for [SDL2](https://www.libsdl.org/) covering 2D rendering
and input, with optional antialiased primitives from SDL2_gfx.

The binding has two layers:

- **`io.github.edadma.sdl2.extern`** — raw `@extern` declarations. The only place
  Scala Native FFI types (`Ptr`, `CInt`, `UByte`, …) appear.
- **`io.github.edadma.sdl2`** — a pure-Scala layer. This is the only package you
  import. It speaks in `Int`, `Boolean`, `String`, and `Color`; native pointers
  are wrapped in zero-cost `AnyVal` value classes (`Window`, `Renderer`,
  `Texture`); memory and C strings are managed for you.

## Requirements

This is a Scala Native–only artifact. You need SDL2 (and SDL2_gfx for the
antialiased primitives) installed and on the linker path:

```sh
brew install sdl2 sdl2_gfx        # macOS
# apt-get install libsdl2-dev libsdl2-gfx-dev   # Debian/Ubuntu
```

## Usage

```scala
libraryDependencies += "io.github.edadma" %%% "sdl2" % "0.0.3"
```

```scala
import io.github.edadma.sdl2.*

@main def run(): Unit =
  setMainReady()
  if !init(INIT_VIDEO) then sys.error(s"SDL_Init failed: $error")

  val window   = createWindow("hello", 800, 600)
  val renderer = window.createRenderer()

  var running = true
  while running do
    var ev = pollEvent()
    while ev.isDefined do
      val e = ev.get
      if e.kind == QUIT then running = false
      else if e.kind == KEYDOWN && e.keyScancode == Scancode.Escape then running = false
      ev = pollEvent()

    renderer.clear(Color.fromRGB(0x0a0a18))
    renderer.fillCircle(400, 300, 80, Color.fromRGB(0x4dabf7)) // antialiased
    renderer.present()

  renderer.destroy(); window.destroy(); quit()
```

Run the bundled demo (bouncing antialiased balls) with `sbt run`.

## Event watches (callback map pattern)

SDL invokes a C function pointer per event. The binding registers a single
trampoline and keeps your Scala closures in a map, so you pass ordinary
functions. Watches fire while events are pumped, on the calling thread.

```scala
val id = addEventWatch { e =>
  if e.kind == KEYDOWN && !e.keyRepeat then println(s"key ${e.keyScancode}")
}
// ...later
removeEventWatch(id)
```

## Antialiasing

`fillCircle`/`aaLine`/`drawCircle` use SDL2_gfx for smooth edges. For a fully
antialiased scene (including straight `drawLine` calls), render into a
supersampled target texture and downscale it with linear filtering:

```scala
setHint(HINT_RENDER_SCALE_QUALITY, "1")
val target = renderer.createTexture(window.pixelFormat, TEXTUREACCESS_TARGET, w * 2, h * 2)
target.setScaleMode(SCALEMODE_LINEAR)
// each frame:
renderer.setTarget(target);  /* draw at 2x */  renderer.resetTarget()
renderer.copy(target); renderer.present()
```

## Coverage

Lifecycle, hints, window, renderer (draw colour/blend, clear, point/line/rect,
render targets, full and positioned `copy`, present), textures (create, create
from surface, scale mode, `size`), surfaces (`width`/`height`/`free`), SDL2_gfx
primitives (`aaLine`, `line`, `drawCircle`, `fillCircle`, `fillBox`), event
polling with field accessors, event watches, and keyboard/mouse state with a
`Scancode` table. The surface/texture bridge supports companion libraries such
as SDL2_ttf. Contributions to widen the surface are welcome.
