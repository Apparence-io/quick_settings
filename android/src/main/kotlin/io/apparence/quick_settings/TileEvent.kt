package io.apparence.quick_settings

import io.apparence.quick_settings.pigeon.Tile

abstract class TileEvent(val tile: Tile?, val callback: ((tile: Tile?) -> Unit)?)

class TileClicked(tile: Tile, callback: (Tile?) -> Unit) : TileEvent(tile, callback)
class TileAdded(tile: Tile, callback: ((Tile?) -> Unit)?) : TileEvent(tile, callback)
class TileRemoved() : TileEvent(null, null)