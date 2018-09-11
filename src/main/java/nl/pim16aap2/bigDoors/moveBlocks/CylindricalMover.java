package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocation;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.MyBlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class CylindricalMover
{
	private BigDoors        	 plugin;
	private World           	 world;
	private boolean     		 instantOpen;
	private FallingBlockFactory_Vall fabf;
	private RotateDirection 	 rotDirection;
	private int             	 dx, dz, xMin, xMax, yMin, yMax, zMin, zMax;
	private List<MyBlockData> savedBlocks = new ArrayList<MyBlockData>();
	private Location        	 turningPoint, pointOpposite;
	@SuppressWarnings("unused")
	private double          	 speed;
	private GetNewLocation  	 gnl;
	private Door            	 door;
	private boolean       	 isASEnabled;
	private double       	 startStepSum;
	private int            	 stepMultiplier;
	
	
	@SuppressWarnings("deprecation")
	public CylindricalMover(BigDoors plugin, World world, int qCircleLimit, RotateDirection rotDirection, double speed,
			Location pointOpposite, DoorDirection currentDirection, Door door, boolean instantOpen)
	{
		this.pointOpposite    = pointOpposite;
		this.turningPoint     = door.getEngine();
		this.rotDirection     = rotDirection;
		this.plugin           = plugin;
		this.world            = world;
		this.door             = door;
		this.isASEnabled      = plugin.isASEnabled();
		this.fabf             = isASEnabled ? plugin.getFABF2() : plugin.getFABF();
		this.instantOpen      = instantOpen;
		this.stepMultiplier   = rotDirection == RotateDirection.CLOCKWISE   ? -1 : 1;
		
		this.xMin    = turningPoint.getBlockX() < pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMin    = turningPoint.getBlockY() < pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMin    = turningPoint.getBlockZ() < pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		this.xMax    = turningPoint.getBlockX() > pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMax    = turningPoint.getBlockY() > pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMax    = turningPoint.getBlockZ() > pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		
		this.speed   = speed;

		this.dx      = pointOpposite.getBlockX() > turningPoint.getBlockX() ? 1 : -1;
		this.dz      = pointOpposite.getBlockZ() > turningPoint.getBlockZ() ? 1 : -1;
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				// Get the radius of this pillar.
				double radius = Math.abs(xAxis - turningPoint.getBlockX()) > Math.abs(zAxis - turningPoint.getBlockZ()) ?
				                Math.abs(xAxis - turningPoint.getBlockX()) : Math.abs(zAxis - turningPoint.getBlockZ());
								
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
					// Move the lowest blocks up a little, so the client won't predict they're touching through the ground, which would make them slower than the rest.
					if (yAxis == yMin)
						newFBlockLocation.setY(newFBlockLocation.getY() + .010001);
					
					Material mat  = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					Byte matData  = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();
					BlockState bs = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getState();
					MaterialData materialData = bs.getData();
					NMSBlock_Vall block  = this.fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
					NMSBlock_Vall block2 = null;
					
					int canRotate        = 0;
					Byte matByte         = matData;
					// Certain blocks cannot be used the way normal blocks can (heads, (ender) chests etc).
					if (Util.isAllowedBlock(mat))
					{
						canRotate        = Util.canRotate(mat);
						// Because I can't get the blocks to rotate properly, they are rotated here
						if (canRotate != 0) 
						{
							Location pos = new Location(world, (int) xAxis, (int) yAxis, (int) zAxis);
							if (canRotate == 1 || canRotate == 3)
								matByte  = rotateBlockDataLog(matData);
							else if (canRotate == 2)
								matByte  = rotateBlockDataStairs(matData);
							
							Block b      = world.getBlockAt(pos);					
							materialData.setData(matByte);
							
							if (plugin.is1_13())
							{
								b.setType(mat);
								BlockState bs2 = b.getState();
								bs2.setData(materialData);
								bs2.update();
								block2 = this.fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
							}
						}
						world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					}
					else
					{
						mat     = Material.AIR;
						matByte = 0;
						matData = 0;
						block   = null;
						materialData = null;
					}
					
					CustomCraftFallingBlock_Vall fBlock = null;
					if (!instantOpen)
						 fBlock = fallingBlockFactory(newFBlockLocation, mat, matData, block);
						
					savedBlocks.add(index, new MyBlockData(mat, matByte, fBlock, radius, materialData, block2 == null ? block : block2, canRotate, -1));
					
					index++;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		
		switch (currentDirection)
		{
		case NORTH:
			this.gnl     = new GetNewLocationNorth(world, xMin, xMax, zMin, zMax, rotDirection);
			startStepSum = Math.PI;
			break;
		case EAST:
			this.gnl     = new GetNewLocationEast (world, xMin, xMax, zMin, zMax, rotDirection);
			startStepSum = Math.PI / 2;
			break;
		case SOUTH:
			this.gnl     = new GetNewLocationSouth(world, xMin, xMax, zMin, zMax, rotDirection);
			startStepSum = 0;
			break;
		case WEST:
			this.gnl     = new GetNewLocationWest (world, xMin, xMax, zMin, zMax, rotDirection);
			startStepSum = 3 * Math.PI / 2;
			break;
		}
		
		if (!instantOpen)
			rotateEntities();
		else
			putBlocks();
	}
	
	// Put the door blocks back, but change their state now.
	@SuppressWarnings("deprecation")
	public void putBlocks()
	{
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					/*
					 * 0-3: Vertical oak, spruce, birch, then jungle 4-7: East/west oak, spruce,
					 * birch, jungle 8-11: North/south oak, spruce, birch, jungle 12-15: Uses oak,
					 * spruce, birch, jungle bark texture on all six faces
					 */

					Material mat    = savedBlocks.get(index).getMat();
					Byte matByte;
					matByte         = savedBlocks.get(index).getBlockByte();
					Location newPos = gnl.getNewLocation(savedBlocks, xAxis, yAxis, zAxis, index);

					if (!instantOpen)
						savedBlocks.get(index).getFBlock().remove();
										
					if (!savedBlocks.get(index).getMat().equals(Material.AIR))
						if (plugin.is1_13())
						{
							savedBlocks.get(index).getBlock().putBlock(newPos);
							Block b = world.getBlockAt(newPos);
							BlockState bs = b.getState();
							bs.update();
						}
						else
						{
							Block b = world.getBlockAt(newPos);
							MaterialData matData = savedBlocks.get(index).getMatData();
							matData.setData(matByte);
							
							b.setType(mat);
							BlockState bs = b.getState();
							bs.setData(matData);
							bs.update();
						}

					index++;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		savedBlocks.clear();

		// Change door availability to true, so it can be opened again.
		// Wait for a bit if instantOpen is enabled.
		if (instantOpen)
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					plugin.getCommander().setDoorAvailable(door.getDoorUID());
				}
			}.runTaskLater(plugin, 40L);
		else
			plugin.getCommander().setDoorAvailable(door.getDoorUID());
	}
	
	// Put falling blocks into their final location (but keep them as falling blocks).
	// This makes the transition from entity to block appear smoother.
	public void finishBlocks()
	{
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{	
					// Get final position of the blocks.
					Location newPos = gnl.getNewLocation(savedBlocks, xAxis, yAxis, zAxis, index);
					
					newPos.setX(newPos.getX() + 0.5);
					newPos.setY(newPos.getY()      );
					newPos.setZ(newPos.getZ() + 0.5);
					
					// Teleport the falling blocks to their final positions.
					savedBlocks.get(index).getFBlock().teleport(newPos);
					savedBlocks.get(index).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					
					++index;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				putBlocks();
			}
		}.runTaskLater(plugin, 4L);
	}
	
	// Method that takes care of the rotation aspect.
	public void rotateEntities()
	{
		new BukkitRunnable()
		{
			Location center   = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
			boolean replace   = false;
			int indexMid      = savedBlocks.size() / 2;
			int counter       = 0;
			int tickRate      = 4;
			double timeToOpen = speed; // seconds.
			int totalTicks    = (int) (20 / tickRate * timeToOpen);
			double step       = (Math.PI / 2) / totalTicks;
			double stepSum    = startStepSum;
			int endCount      = (int) (totalTicks * 1.05);
			int replaceCount  = (int) (endCount / 2);
			
			@Override
			public void run()
			{
				if (counter == 0 || (counter * tickRate < endCount * tickRate - 27 && counter % 4 == 0))
					Util.playSound(door.getEngine(), "bd.dragging2", 0.8f, 0.6f);
				int index = 0;
				
				if (!plugin.getCommander().isPaused())
					++counter;
				stepSum = startStepSum + step * stepMultiplier * counter;

				replace = false;
				if (counter == replaceCount)
					replace = true;
				
				if (!plugin.getCommander().canGo() || counter > endCount)
				{					
					Util.playSound(door.getEngine(), "bd.closing-vault-door", 0.85f, 1f);
					for (int idx = 0; idx < savedBlocks.size(); ++idx)
						savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					finishBlocks();
					this.cancel();
				}
				else
				{
					double xAxis = turningPoint.getX();
					do
					{
						double zAxis = turningPoint.getZ();
						do
						{
							double radius     = savedBlocks.get(index).getRadius();
							for (double yAxis = yMin; yAxis <= yMax; yAxis++)
							{
								// It is not pssible to edit falling block blockdata (client won't update it), so delete the current fBlock and replace it by one that's been rotated. 
								if (replace)
								{
									if (savedBlocks.get(index).canRot() != 0)
									{
										Material mat = savedBlocks.get(index).getMat();
										Location loc = savedBlocks.get(index).getFBlock().getLocation();
										Byte matData = savedBlocks.get(index).getBlockByte();
										Vector veloc = savedBlocks.get(index).getFBlock().getVelocity();
										if (yAxis != yMin)
											loc.setY(loc.getY() - .010001);
										CustomCraftFallingBlock_Vall fBlock;
										// Because the block in savedBlocks is already rotated where applicable, just use that block now.
										fBlock = fallingBlockFactory(loc, mat, (byte) matData, savedBlocks.get(index).getBlock());
										
										savedBlocks.get(index).getFBlock().remove();
										savedBlocks.get(index).setFBlock(fBlock);
										savedBlocks.get(index).getFBlock().setVelocity(veloc);
									}
								}
								
								if (isASEnabled)
								{
									Location vec;
									if (radius != 0)
										vec = (new Location(center.getWorld(), center.getX(), yAxis, center.getZ())).subtract(savedBlocks.get(index).getFBlock().getLocation());
									else
									{
										Location midLoc = savedBlocks.get(indexMid).getFBlock().getLocation();
										vec = (new Location(center.getWorld(), midLoc.getX(), yAxis, midLoc.getZ())).subtract(savedBlocks.get(index).getFBlock().getLocation());
									}
									savedBlocks.get(index).getFBlock().setHeadPose(directionToEuler(vec));
								}
								
								if (radius != 0)
								{
									Location loc;
									double addX = radius * Math.sin(stepSum);
									double addY = 0;
									double addZ = radius * Math.cos(stepSum);
									
									loc = new Location(null, center.getX() + addX,
											                 yAxis         + addY, 
											                 center.getZ() + addZ);
								
									Vector vec = loc.toVector().subtract(savedBlocks.get(index).getFBlock().getLocation().toVector());
									vec.multiply(0.101);
									savedBlocks.get(index).getFBlock().setVelocity(vec);
								}
								index++;
							}
							zAxis += dz;
						}
						while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
						xAxis += dx;
					}
					while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
					replace = false;
				}
			}
		}.runTaskTimer(plugin, 14, 4);
	}
	
	// Rotate logs by modifying its material data.
	public byte rotateBlockDataLog(Byte matData)
	{
		if (matData >= 4 && matData <= 7)
			matData = (byte) (matData + 4);
		else if (matData >= 7 && matData <= 11)
			matData = (byte) (matData - 4);
		return matData;
	}
	
	private EulerAngle directionToEuler(Location dir) 
	{
//	    double xzLength =  Math.sqrt(dir.getX() * dir.getX()  + dir.getZ() * dir.getZ());
//	    double pitch    =  Math.atan2(xzLength,   dir.getY()) - Math.PI / 2;
	    double yaw      = -Math.atan2(dir.getX(), dir.getZ()) + Math.PI / 2;
	    return new EulerAngle(0, yaw, 0);
	}
	
	// Rotate stairs by modifying its material data.
	public byte rotateBlockDataStairs(Byte matData)
	{
		if (this.rotDirection == RotateDirection.CLOCKWISE)
		{
			if (matData == 0 || matData == 4)
				matData = (byte) (matData + 2);
			else if (matData == 1 || matData == 5)
				matData = (byte) (matData + 2);
			else if (matData == 2 || matData == 6)
				matData = (byte) (matData - 1);
			else if (matData == 3 || matData == 7)
				matData = (byte) (matData - 3);
		}
		else
		{
			if (matData == 0 || matData == 4)
				matData = (byte) (matData + 3);
			else if (matData == 1 || matData == 5)
				matData = (byte) (matData + 1);
			else if (matData == 2 || matData == 6)
				matData = (byte) (matData - 2);
			else if (matData == 3 || matData == 7)
				matData = (byte) (matData - 2);
		}
		return matData;
	}
	
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, NMSBlock_Vall block)
	{		
		CustomCraftFallingBlock_Vall entity = this.fabf.fallingBlockFactory(loc, block, matData, mat);
		Entity bukkitEntity = (Entity) entity;
		bukkitEntity.setCustomName("BigDoorsEntity");
		bukkitEntity.setCustomNameVisible(false);
		return entity;
	}
}
