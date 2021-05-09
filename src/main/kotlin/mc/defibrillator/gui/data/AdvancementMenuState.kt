package mc.defibrillator.gui.data

import mc.defibrillator.gui.AdvancementScreenHandlerFactory
import mc.defibrillator.util.classes.DynamicLimitedIntProp
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AdvancementMenuState(val targetUUID: UUID, val player: ServerPlayerEntity) {
    lateinit var factory: AdvancementScreenHandlerFactory
    var suppressOnClose: AtomicBoolean = AtomicBoolean(false)

    var size = 0
    var page by DynamicLimitedIntProp({ 0 }, { size / PER_PAGE })

    var overrides: HashMap<Identifier, Boolean> = hashMapOf()

    companion object {
        const val PER_PAGE = 9 * 5
    }
}
