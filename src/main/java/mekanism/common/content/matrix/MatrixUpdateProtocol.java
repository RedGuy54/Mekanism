package mekanism.common.content.matrix;

import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.util.StackUtils;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.multiblock.MultiblockCache;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.multiblock.UpdateProtocol;
import mekanism.common.tile.TileEntityInductionCasing;
import mekanism.common.tile.TileEntityInductionCell;
import mekanism.common.tile.TileEntityInductionProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class MatrixUpdateProtocol extends UpdateProtocol<SynchronizedMatrixData>
{
	public MatrixUpdateProtocol(TileEntityInductionCasing tileEntity) 
	{
		super(tileEntity);
	}

	@Override
	protected boolean isValidFrame(int x, int y, int z) 
	{
		return pointer.getWorldObj().getBlock(x, y, z) == MekanismBlocks.BasicBlock2 && pointer.getWorldObj().getBlockMetadata(x, y, z) == 1;
	}
	
	@Override
	public boolean isValidInnerNode(int x, int y, int z)
	{
		TileEntity tile = pointer.getWorldObj().getTileEntity(x, y, z);
		
		if(tile != null && (tile instanceof TileEntityInductionCell || tile instanceof TileEntityInductionProvider))
		{
			return true;
		}
		
		return isAir(x, y, z);
	}

	@Override
	protected MatrixCache getNewCache() 
	{
		return new MatrixCache();
	}

	@Override
	protected SynchronizedMatrixData getNewStructure() 
	{
		return new SynchronizedMatrixData();
	}

	@Override
	protected MultiblockManager<SynchronizedMatrixData> getManager() 
	{
		return Mekanism.matrixManager;
	}

	@Override
	protected void mergeCaches(List<ItemStack> rejectedItems, MultiblockCache<SynchronizedMatrixData> cache, MultiblockCache<SynchronizedMatrixData> merge) 
	{
		List<ItemStack> rejects = StackUtils.getMergeRejects(((MatrixCache)cache).inventory, ((MatrixCache)merge).inventory);
		
		if(!rejects.isEmpty())
		{
			rejectedItems.addAll(rejects);
		}
		
		StackUtils.merge(((MatrixCache)cache).inventory, ((MatrixCache)merge).inventory);
	}
	
	@Override
	protected void onFormed()
	{
		for(Coord4D coord : innerNodes)
		{
			TileEntity tile = coord.getTileEntity(pointer.getWorldObj());
			
			if(tile instanceof TileEntityInductionCell)
			{
				structureFound.cells.add(coord);
				structureFound.storageCap += ((TileEntityInductionCell)tile).tier.MAX_ELECTRICITY;
				structureFound.electricityStored += ((TileEntityInductionCell)tile).getEnergy();
				((TileEntityInductionCell)tile).setEnergy(0);
			}
			else if(tile instanceof TileEntityInductionProvider)
			{
				structureFound.providers.add(coord);
				structureFound.outputCap += ((TileEntityInductionProvider)tile).tier.OUTPUT;
			}
		}
	}
	
	@Override
	protected void onStructureDestroyed(SynchronizedMatrixData structure)
	{
		if(structure.electricityStored <= 0)
		{
			return;
		}
		
		for(Coord4D coord : structure.cells)
		{
			if(coord.getTileEntity(pointer.getWorldObj()) instanceof TileEntityInductionCell)
			{
				TileEntityInductionCell cell = (TileEntityInductionCell)coord.getTileEntity(pointer.getWorldObj());
				
				double toAdd = Math.min(cell.getMaxEnergy()-cell.getEnergy(), structure.electricityStored);
				cell.setEnergy(cell.getEnergy()+toAdd);
				structure.electricityStored -= toAdd;
			}
			
			if(structure.electricityStored <= 0)
			{
				break;
			}
		}
	}
}
