/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mc.defibrillator.dimension

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.ChunkRegion
import net.minecraft.world.HeightLimitView
import net.minecraft.world.Heightmap
import net.minecraft.world.biome.source.BiomeSource
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.gen.StructureAccessor
import net.minecraft.world.gen.chunk.ChunkGenerator
import net.minecraft.world.gen.chunk.StructuresConfig
import net.minecraft.world.gen.chunk.VerticalBlockSample
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Function

class EmptyChunkGenerator(biomeSource: BiomeSource?) : ChunkGenerator(biomeSource, StructuresConfig(false)) {
    override fun getCodec(): Codec<out ChunkGenerator> {
        return CODEC
    }

    override fun withSeed(seed: Long): ChunkGenerator {
        return this
    }

    override fun buildSurface(region: ChunkRegion, chunk: Chunk) {}
    override fun populateNoise(
        executor: Executor,
        accessor: StructureAccessor,
        chunk: Chunk
    ): CompletableFuture<Chunk> {
        return CompletableFuture.completedFuture(chunk)
    }

    override fun getHeight(x: Int, z: Int, heightmap: Heightmap.Type?, world: HeightLimitView?): Int {
        return 0
    }

    override fun getColumnSample(x: Int, z: Int, world: HeightLimitView?): VerticalBlockSample {
        return VerticalBlockSample(0, arrayOfNulls(0))
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
