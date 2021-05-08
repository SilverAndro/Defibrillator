package mc.defibrillator.util

import mc.defibrillator.mixin.access.AdvancementProgressAccessor
import net.minecraft.advancement.AdvancementProgress
import net.minecraft.advancement.criterion.CriterionProgress

fun AdvancementProgress.requirements(): Array<out Array<String>> {
    return (this as AdvancementProgressAccessor).requirements
}

fun AdvancementProgress.criteriaProgress(): MutableMap<String, CriterionProgress> {
    return (this as AdvancementProgressAccessor).criteriaProgresses
}
