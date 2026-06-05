package io.github.edadma.sdl2.extern

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Raw `@extern` bindings to SDL2_gfx — antialiased and filled primitives that
  * the base SDL renderer lacks. Coordinates are `Sint16` (CShort). Wrapped by
  * the high-level `Renderer` methods (`aaLine`, `fillCircle`, …).
  */
@link("SDL2_gfx")
@extern
object LibSDL2Gfx:
  def pixelRGBA(r: Ptr[Byte], x: CShort, y: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt = extern
  def lineRGBA(r: Ptr[Byte], x1: CShort, y1: CShort, x2: CShort, y2: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt   = extern
  def aalineRGBA(r: Ptr[Byte], x1: CShort, y1: CShort, x2: CShort, y2: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt = extern
  def thickLineRGBA(r: Ptr[Byte], x1: CShort, y1: CShort, x2: CShort, y2: CShort, width: UByte, red: UByte, g: UByte, b: UByte, a: UByte): CInt = extern
  def circleRGBA(r: Ptr[Byte], x: CShort, y: CShort, rad: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt       = extern
  def aacircleRGBA(r: Ptr[Byte], x: CShort, y: CShort, rad: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt     = extern
  def filledCircleRGBA(r: Ptr[Byte], x: CShort, y: CShort, rad: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt = extern
  def rectangleRGBA(r: Ptr[Byte], x1: CShort, y1: CShort, x2: CShort, y2: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt = extern
  def boxRGBA(r: Ptr[Byte], x1: CShort, y1: CShort, x2: CShort, y2: CShort, red: UByte, g: UByte, b: UByte, a: UByte): CInt      = extern
