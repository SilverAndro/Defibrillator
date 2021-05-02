package mc.defibrillator.gui.data

import mc.defibrillator.gui.NBTScreenHandlerFactory
import mc.defibrillator.util.classes.DynamicLimitedIntProp
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AdvancementMenuState(val targetUUID: UUID, val player: ServerPlayerEntity) {
    var factory: NBTScreenHandlerFactory? = null
    var suppressOnClose: AtomicBoolean = AtomicBoolean(false)

    var size = 0
    var page by DynamicLimitedIntProp({ 0 }, { size / PER_PAGE })

    companion object {
        const val PER_PAGE = 9 * 5
    }
}
