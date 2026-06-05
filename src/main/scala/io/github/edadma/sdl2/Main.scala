package io.github.edadma.sdl2

/** A small showcase using only the pure-Scala layer: a window of antialiased
  * balls bouncing off the walls, with an event watch (the callback map pattern)
  * logging key presses. Drag is ignored; press Escape or close the window to
  * quit. Run with `sbt run`.
  */
@main def run(): Unit =
  val width  = 800
  val height = 600

  setMainReady()
  if !init(INIT_VIDEO) then sys.error(s"SDL_Init failed: $error")
  setHint(HINT_RENDER_SCALE_QUALITY, "1")

  val window   = createWindow("sdl2 — bouncing balls", width, height)
  if window.isNull then sys.error(s"createWindow failed: $error")
  val renderer = window.createRenderer()

  // Antialias by supersampling: draw into a 2x target texture and downscale it
  // with linear filtering on copy. SDL2_gfx's aacircle edge is barely visible;
  // this smooths everything uniformly, like the JVM/canvas renderers.
  val ss     = 2
  val texW   = width * ss
  val texH   = height * ss
  val target = renderer.createTexture(window.pixelFormat, TEXTUREACCESS_TARGET, texW, texH)
  target.setScaleMode(SCALEMODE_LINEAR)

  // The map pattern in action: an ordinary Scala closure bridged to SDL's C
  // event callback. Fired while events are pumped, on this thread.
  addEventWatch { e =>
    if e.kind == KEYDOWN && !e.keyRepeat then println(s"key down: scancode ${e.keyScancode}")
  }

  val rng     = new scala.util.Random(42)
  val palette = Vector(0xff6b6b, 0xffd43b, 0x69db7c, 0x4dabf7, 0xda77f2, 0xff922b).map(Color.fromRGB)
  val n       = 14
  val xs      = Array.fill(n)(rng.between(60.0, width - 60.0))
  val ys      = Array.fill(n)(rng.between(60.0, height - 60.0))
  val vxs     = Array.fill(n)(rng.between(-4.0, 4.0))
  val vys     = Array.fill(n)(rng.between(-4.0, 4.0))
  val rs      = Array.fill(n)(rng.between(14, 40))
  val cs      = Array.tabulate(n)(i => palette(i % palette.length))
  val bg      = Color.fromRGB(0x0a0a18)

  var running = true
  while running do
    var ev = pollEvent()
    while ev.isDefined do
      val e = ev.get
      if e.kind == QUIT then running = false
      else if e.kind == KEYDOWN && e.keyScancode == Scancode.Escape then running = false
      ev = pollEvent()

    // Integrate, then bounce off the walls.
    var i = 0
    while i < n do
      xs(i) += vxs(i)
      ys(i) += vys(i)
      val r = rs(i)
      if xs(i) < r then { xs(i) = r; vxs(i) = -vxs(i) }
      if xs(i) > width - r then { xs(i) = width - r; vxs(i) = -vxs(i) }
      if ys(i) < r then { ys(i) = r; vys(i) = -vys(i) }
      if ys(i) > height - r then { ys(i) = height - r; vys(i) = -vys(i) }
      i += 1

    // Elastic ball-ball collisions (mass ∝ area). O(n²), tiny n.
    i = 0
    while i < n do
      var j = i + 1
      while j < n do
        val dx      = xs(j) - xs(i)
        val dy      = ys(j) - ys(i)
        val dist    = math.sqrt(dx * dx + dy * dy)
        val minDist = (rs(i) + rs(j)).toDouble
        if dist > 0.0 && dist < minDist then
          val nx = dx / dist
          val ny = dy / dist
          val mi = (rs(i) * rs(i)).toDouble
          val mj = (rs(j) * rs(j)).toDouble
          // Separate the overlap so they don't stick, split by mass.
          val overlap = minDist - dist
          xs(i) -= nx * overlap * (mj / (mi + mj)); ys(i) -= ny * overlap * (mj / (mi + mj))
          xs(j) += nx * overlap * (mi / (mi + mj)); ys(j) += ny * overlap * (mi / (mi + mj))
          // Exchange momentum along the normal only if they're approaching.
          val vn = (vxs(i) - vxs(j)) * nx + (vys(i) - vys(j)) * ny
          if vn > 0.0 then
            val k = 2.0 * vn / (mi + mj)
            vxs(i) -= k * mj * nx; vys(i) -= k * mj * ny
            vxs(j) += k * mi * nx; vys(j) += k * mi * ny
        j += 1
      i += 1

    // Draw into the 2x target, then downscale to the window (antialiasing).
    renderer.setTarget(target)
    renderer.clear(bg)
    i = 0
    while i < n do
      renderer.fillCircle((xs(i) * ss).toInt, (ys(i) * ss).toInt, rs(i) * ss, cs(i))
      i += 1
    renderer.resetTarget()
    renderer.copy(target)
    renderer.present()

  target.destroy()
  renderer.destroy()
  window.destroy()
  quit()
