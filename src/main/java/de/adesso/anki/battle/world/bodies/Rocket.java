package de.adesso.anki.battle.world.bodies;

import de.adesso.anki.battle.mqtt.MqttService;
import de.adesso.anki.battle.util.Position;
import de.adesso.anki.battle.world.Body;
import de.adesso.anki.battle.world.DynamicBody;
import de.adesso.anki.battle.world.World;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

public class Rocket extends DynamicBody {
//TODO should rockets follow the roadmap ? 
	// else explode after x seconds for garbage collector
	
	// hotfix for driving into own rockets
	private long timer;
	
	public Rocket (String direction) {
		timer = System.currentTimeMillis();
	}
	
	
	
	// TODO adjust timers
	public boolean isActive () {
		return System.currentTimeMillis() > timer + 200 ;	
	}
	public boolean shouldExplode () {
		return System.currentTimeMillis() > timer + 8000 ;
	}

	@Override
	public void evaluateBehavior(MqttService mqtt) throws MqttException {
		World world = this.getWorld();
		if ( checkCollision(this,world)) {
			world.getBodiesModifiable().remove(this);
		}
	}
	
		// maybe uplift
		private boolean checkCollision(Body weapon, World world) {
			
			//merge into weapon superclass
	    	if (weapon instanceof Rocket && !((Rocket) weapon).isActive()) {
	    		return false;
	    	}
	    	List<Vehicle> vehicles = world.getVehicles();
			Position pos1 = weapon.getPosition();
			boolean destroy = false;
			//TODO find damage values for weapon types
			int damage = 10;
	    	for (Vehicle vehicle : vehicles) {
				Position pos2 = vehicle.getPosition();
				double distance = pos1.distance(pos2);
				//TODO find distance value that indicates a collision
				double dummyValue = 30; 
				if (distance < dummyValue) {
					vehicle.setEnergy(vehicle.getEnergy() - damage);
					destroy = true;
				}
	    	}

			return destroy;
	    }
	
}
