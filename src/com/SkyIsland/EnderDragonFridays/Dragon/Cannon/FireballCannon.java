package com.SkyIsland.EnderDragonFridays.Dragon.Cannon;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.SkyIsland.EnderDragonFridays.EnderDragonFridaysPlugin;
import com.SkyIsland.EnderDragonFridays.Dragon.Dragon;
import com.SkyIsland.EnderDragonFridays.Dragon.Cannon.Events.FireFireballEvent;

/**
 * 
 * @author Skyler
 * @note This could be extracted out to 'body part' class. There could be two. One that's independent and one that just
 * 			exists. I.e. one that extends thread and one that doens't get it's own.
 */
public class FireballCannon extends Cannon {
	
	private double d_min;
	private double d_max;
	private double d_incr;
	private int d_incr_range;
	private Random rand;
	private Dragon dragon;
	private TargetType targetType;
	private double offsetX, offsetY, offsetZ;
	
	/**
	 * Creates a fireball cannon with the provided min and max delay. Increments default to 1/10 the range.
	 * The offset from where the fireballs are launched relative to the dragon is defaulted to 0. <u>This
	 * is not encouraged</u>, as multiple fireballs spawned at the same location either explode or bounce off
	 * in a random direction.
	 * @param dragon The {@link com.SkyIsland.EnderDragonFridays.Dragon.Dragon Dragon} to fire cannonballs from.
	 * @param type What {@link com.SkyIsland.EnderDragonFridays.Dragon.Cannon.TargetType targeting type} is used.
	 * @param min_delay The smallest amount of time (<b>in ticks</b>) to wait before firing again
	 * @param max_delay The largest amount of time (<b>also in ticks</b>) to wait before firing again
	 */
	public FireballCannon(Dragon dragon, TargetType type, double min_delay, double max_delay) {
		this(dragon, type, min_delay, max_delay, (max_delay-min_delay)/10); //default to 1/10 the range as increments
	}
	
	/**
	 * Creates a cannon with the passed delay and increments.
	 * The offset from where the fireballs are launched relative to the dragon is defaulted to 0. <u>This
	 * is not encouraged</u>, as multiple fireballs spawned at the same location either explode or bounce off
	 * in a random direction.
	 * @param dragon The {@link com.SkyIsland.EnderDragonFridays.Dragon.Dragon Dragon} to fire cannonballs from.
	 * @param type What {@link com.SkyIsland.EnderDragonFridays.Dragon.Cannon.TargetType targeting type} is used.
	 * @param min_delay The smallest amount of time (<b>in ticks</b>) to wait before firing again
	 * @param max_delay The largest amount of time (<b>also in ticks</b>) to wait before firing again
	 * @param increments The smallest amount of time (<b>again, in ticks</b>) in-between different firing times.
	 */
	public FireballCannon(Dragon dragon, TargetType type, double min_delay, double max_delay, double increments) {
		this(dragon, type, min_delay, max_delay, increments, 0.0, 0.0, 0.0);
	}
	
	/**
	 * Creates a cannon with the passed delay and increments.
	 * The offset from where the fireballs are launched relative to the dragon is defaulted to 0. <u>This
	 * is not encouraged</u>, as multiple fireballs spawned at the same location either explode or bounce off
	 * in a random direction.
	 * @param dragon The {@link com.SkyIsland.EnderDragonFridays.Dragon.Dragon Dragon} to fire cannonballs from.
	 * @param type What {@link com.SkyIsland.EnderDragonFridays.Dragon.Cannon.TargetType targeting type} is used.
	 * @param min_delay The smallest amount of time (<b>in ticks</b>) to wait before firing again
	 * @param max_delay The largest amount of time (<b>also in ticks</b>) to wait before firing again
	 * @param increments The smallest amount of time (<b>again, in ticks</b>) in-between different firing times.
	 * @param offsetX The offset the fireball will be created from in relation to the dragon
	 * @param offsetY The offset the fireball will be created from in relation to the dragon
	 * @param offsetZ The offset the fireball will be created from in relation to the dragon
	 */
	public FireballCannon(Dragon dragon, TargetType type, double min_delay, double max_delay, double increments, double offsetX, double offsetY, double offsetZ) {
		this.dragon = dragon;
		this.targetType = type;
		
		this.d_min = min_delay;
		this.d_max = max_delay;
		
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		
		//make sure min and max aren't reversed
		if (d_min > d_max) {
			d_max = min_delay;
			d_min = max_delay;
		}
		
		//make sure increments aren't negative
		d_incr = Math.abs(increments);
		
		//we need to make sure the increments are sound
		if ( ((d_max - d_min) / d_incr) % 1 <= .0001) {
			//if increments doesn't 'evenly' divide our range, we're in trouble
			//so we'll round to nearest increment that would
			float even = (int) Math.round((d_max - d_min) / d_incr);
			d_incr = (d_max - d_min) / even; 
		}
		
		d_incr_range = (int) Math.round((d_max - d_min) / d_incr); //store so we don't have to do this math over and over again
		
		//create out random
		rand = new Random();
		
		Long time = (long) (d_min + (d_incr * (rand.nextInt(d_incr_range)))); 
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(EnderDragonFridaysPlugin.plugin, this, time);
	}
	
	@Override
	public void run() {
		LivingEntity dDragon = dragon.getDragon();

		//very first, make sure dragon is still alive. If not, kill self
		if (dragon == null || !dragon.isAlive()) {
			return;
		}
		Long time = (long) (d_min + (d_incr * (rand.nextInt(d_incr_range))));
		
		//make sure there are players to fire at
		if (dDragon.getWorld().getPlayers().isEmpty()) {
			//reschedule the task for a later time
			Bukkit.getScheduler().scheduleSyncDelayedTask(EnderDragonFridaysPlugin.plugin, this, time);
			return;
		}
		
		//there are players in the world
		
		//use our targetType to figure out who we want to attack
		Player target = null;
		
		switch (targetType) {
		case MOSTDAMAGE:
			//try and get who has done the most damage
			target = dragon.getMostDamage();
			if (target != null) {
				break;
			}
			//if target is null, we want it to operate like NEAREST. So we don't break and instead continue;
		case NEAREST:
		default:
			boolean go = true;
			Entity next;
			for (int i = 10; go == true && i < 200; i++) {
				List<Entity> players = dragon.getDragon().getNearbyEntities(i, 150, i);
				if (players.isEmpty())
					continue;
				Iterator<Entity> it = players.iterator();
				while (it.hasNext()) {
					next = it.next();
					if (next instanceof Player) {
						//we found a player
						target = (Player) next;
						go = false;
						break;
					}
				}
				//implicit else: continue
				//really hoping this doesn't throw concurrent modification. This is on a separate thread
			}
			break;
		}
		
		
//		if (target == null) {
//			//nobody has hit it yet
//			List<Player> players = new ArrayList<Player>(dDragon.getWorld().getPlayers()); //attempt to get rid of concurrent modification
//			if (!players.isEmpty())
//				target = players.get(rand.nextInt(players.size()));
//			else {
//				return; //no players in world. keep sleeping.
//			}
//		}
		
		
		//reschedule this event to run
		Bukkit.getScheduler().scheduleSyncDelayedTask(EnderDragonFridaysPlugin.plugin, this, time);
		
		if (target != null) {
			//actually launch fireball if we have a target
			Bukkit.getPluginManager().callEvent(new FireFireballEvent(dragon.getDragon(), target, dragon.getDragon().getLocation().add(offsetX, offsetY, offsetZ)));
		}
		
	}
}