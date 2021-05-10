package mc.defibrillator.dimension

import net.minecraft.world.biome.source.BiomeSource
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.chunk.StructuresConfig
import mc.defibrillator.dimension.EmptyChunkGenerator
import net.minecraft.world.ChunkRegion
import net.minecraft.world.WorldAccess
import net.minecraft.world.gen.StructureAccessor
import net.minecraft.world.Heightmap
import net.minecraft.world.gen.chunk.VerticalBlockSample
import net.minecraft.block.BlockState
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.mojang.datafixers.kinds.App
import com.mojang.serialization.Codec
import net.minecraft.world.BlockView
import net.minecraft.world.chunk.Chunk
import java.util.function.Function

class EmptyChunkGenerator(biomeSource: BiomeSource?) : ChunkGenerator(biomeSource, StructuresConfig(false)) {
    override fun getCodec(): Codec<out ChunkGenerator> {
        return CODEC
    }

    override fun withSeed(seed: Long): ChunkGenerator {
        return this
    }

    override fun buildSurface(region: ChunkRegion, chunk: Chunk) {}
    override fun populateNoise(world: WorldAccess, accessor: StructureAccessor, chunk: Chunk) {}
    override fun getHeight(x: Int, z: Int, heightmapType: Heightmap.Type): Int {
        return 0
    }

    override fun getColumnSample(x: Int, z: Int): BlockView {
        return VerticalBlockSample(arrayOfNulls(0))
    }

    companion object {
        val CODEC: Codec<EmptyChunkGenerator> = RecordCodecBuilder.create { instance ->
            instance.group(
                BiomeSource.CODEC.fieldOf("biome_source")
                    .forGetter { generator: EmptyChunkGenerator -> generator.biomeSource }
            )
                .apply(
                    instance, instance.stable(
                        Function { biomeSource: BiomeSource -> EmptyChunkGenerator(biomeSource) })
                )
        }
    }
}
