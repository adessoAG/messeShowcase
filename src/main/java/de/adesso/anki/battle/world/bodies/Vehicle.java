package de.adesso.anki.battle.world.bodies;

import com.commands.Command;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.states.GameState;
import de.adesso.anki.battle.mqtt.MqttService;
import de.adesso.anki.battle.util.Position;
import de.adesso.anki.battle.world.DynamicBody;
import de.adesso.anki.battle.world.bodies.roadpieces.FinishRoadpiece;
import de.adesso.anki.battle.world.bodies.roadpieces.Roadpiece;
import de.adesso.anki.battle.world.bodies.roadpieces.StartRoadpiece;
import de.adesso.anki.sdk.AnkiVehicle;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Vehicle extends DynamicBody {

	private String name;
    private Roadpiece currentRoadpiece;

    private Long startLapTime = System.currentTimeMillis();
    private Long currentlapTime;
    private Long bestLapTime = 1111111111111111L;  // TODO maximum 
    // System.nanoTime(
    



	//TODO: Bei Gelegenheit in 3 Pakete unterteilen
	private List<GameState> factsRoad;
	private List<GameState> factsInventory;
	private List<GameState> factsObstacles;

	private int track ; 		
	private Command nextCommand;
	private boolean rocketReady;
	private boolean mineReady;
	private boolean shieldReady;
	private boolean reflectorReady;
    private AnkiVehicle ankiReference;


	public double getOffset() {
		return offsetFromCenter;
	}

	private double offsetFromCenter;
	private double targetOffset;

	private double horizontalSpeed = 80;

	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public boolean isMineReady() {
		return mineReady;
	}

	public void setMineReady(boolean mineReady) {
		this.mineReady = mineReady;
	}

	public boolean isShieldReady() {
		return shieldReady;
	}

	public void setShieldReady(boolean shieldReady) {
		this.shieldReady = shieldReady;
	}
	
	public Long getCurrentlapTime() {
		return currentlapTime;
	}
	public void setCurrentlapTime(Long currentlapTime) {
		this.currentlapTime = currentlapTime;
	}

	public boolean isReflectorReady() {
		return reflectorReady;
	}

	public void setReflectorReady(boolean reflectorReady) {
		this.reflectorReady = reflectorReady;
	}

	public boolean isRocketReady() {
		return rocketReady;
	}

	public void setRocketReady(boolean rocketReady) {
		this.rocketReady = rocketReady;
	}

	public Vehicle() {	
	}

	public int getTrack() {
		return this.track;
	}
	
	
	public void setTrack (int track) {
		this.track = track;
	}
	
    public Long getstartLapTime() {
		return startLapTime;
	}
	public void setstartLapTime(Long lapTime) {
		this.startLapTime = lapTime;
	}
	
	

	public void setTargetOffset(double offsetFromCenter) {
		targetOffset = offsetFromCenter;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
		// TODO Auto-generated method stub
	}

	public void setCalibrationOffset(double newOffset) {
    	val deltaOffset = newOffset - offsetFromCenter;
    	if (deltaOffset != 0) {
    		if (position != null)
				position = position.transform(Position.at(0, -deltaOffset));
			offsetFromCenter += deltaOffset;
		}
	}

    @Override
    public void updatePosition(long deltaNanos) {
        if (position != null) {
        	updateOffset(deltaNanos);
			updateForwardPosition(deltaNanos);
        }
    }

    private void updateOffset(long deltaNanos) {
		double deltaOffset = 0;

		if (offsetFromCenter < targetOffset && speed > horizontalSpeed) {
			deltaOffset = horizontalSpeed * deltaNanos / 1_000_000_000;
			deltaOffset = Math.min(deltaOffset, targetOffset - offsetFromCenter);
		}

		if (offsetFromCenter > targetOffset && speed > horizontalSpeed) {
			deltaOffset = -horizontalSpeed * deltaNanos / 1_000_000_000;
			deltaOffset = Math.max(deltaOffset, targetOffset - offsetFromCenter);
		}

		if (deltaOffset != 0) {
			position = position.transform(Position.at(0, -deltaOffset));
			offsetFromCenter += deltaOffset;
		}
	}

	private void updateForwardPosition(long deltaNanos) {
		if (ankiReference == null) {
			if (speed < targetSpeed) {
				speed += acceleration * deltaNanos / 1_000_000_000;
				speed = Math.min(speed, targetSpeed);
			}

			if (speed > targetSpeed) {
				speed -= acceleration * deltaNanos / 1_000_000_000;
				speed = Math.max(speed, targetSpeed);
			}
		}

		double travel = speed * deltaNanos / 1_000_000_000;

		val oldRoadpiece = currentRoadpiece;

		if (currentRoadpiece != null && position != null) {
            while (travel > 0) {
                double maxTravel = currentRoadpiece.findMaximumTravel(position);
                if (travel <= maxTravel) {
                    position = currentRoadpiece.followTrack(position, travel);
                    travel = 0;
                } else {
                    position = currentRoadpiece.followTrack(position, maxTravel);
                    travel -= maxTravel;
                    currentRoadpiece = currentRoadpiece.getNext();
                }
            }
        }

        if (ankiReference != null)
        	currentRoadpiece = oldRoadpiece;
	}

    @Override
    public void setFacts(List <GameState> factsRoad, List <GameState> factsInventory,
    											List <GameState> factsObstacles)
    {
    	this.factsRoad = factsRoad ;
    	this.factsInventory =factsInventory ;
    	this.factsObstacles = factsObstacles;

    }

    public String convertFactsToMessage() {
    	ObjectMapper objMapper = new ObjectMapper();

		JSONObject json = new JSONObject();
		JSONArray arr = new JSONArray();
    	try {
			json.put("speed", this.speed);
			for (GameState gameState : this.factsRoad) {
	    		arr.put(gameState.getClass().getSimpleName());
			}
			json.put("nextRoadPiece", arr);


			arr = new JSONArray();
			for (GameState gameState : this.factsInventory) {
				arr.put(gameState.getClass().getSimpleName());
			}
    		json.put("inv",arr);


			//arr = new JSONArray();
			//for (GameState gameState : this.factsObstacles) {
			//	arr.put(gameState.getClass().getSimpleName());
			//}
    		//json.put("obstacles",arr);
    		return json.toString();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return "";
    }

    @Override
    public void evaluateBehavior(MqttService mqtt) throws MqttException {
    	

    	String topic = this.name;
    	String message ="{\"speed\":\"100\",\"nextRoadPiece\":\"left\",\"inv\":\"mine\"}";
    	// generate Message from facts
    	String messageToSend = convertFactsToMessage();
    	//mqtt.publish(topic, message);
    	mqtt.publish(topic, messageToSend);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "roadpiece=" + currentRoadpiece +
                ", position=" + position +
				", speed=" + String.format(Locale.ROOT, "%.1f", speed) +
				", offset=" + String.format(Locale.ROOT, "%.1f", offsetFromCenter) +
                '}';
    }

    public void setCurrentRoadpiece(Roadpiece roadpiece) {
        currentRoadpiece = roadpiece;
    }


    public void setAnkiReference(AnkiVehicle ankiReference) {
        this.ankiReference = ankiReference;
    }

	public AnkiVehicle getAnkiReference() {
		return ankiReference;
	}

	public double getTargetOffset() {
		return targetOffset;
	}

    public Roadpiece getCurrentRoadpiece() {
        return currentRoadpiece;
    }

	public double getHorizontalSpeed() {
		return horizontalSpeed;
	}
	public void updateLapTime() {
		if (this.currentRoadpiece.toString().equals("S!"))   {
			this.startLapTime = System.currentTimeMillis(); 
			System.out.println("Start");
		}
		Long currentTime = System.currentTimeMillis(); 
		this.currentlapTime = currentTime - this.startLapTime  ;
		if (currentRoadpiece.toString().equals( "F!")) {
			this.startLapTime = System.currentTimeMillis(); 
			if (this.currentlapTime < bestLapTime) {
				this.bestLapTime  = this.currentlapTime;
			}
			log.debug("finish");
		}
		Long lapTime = this.currentlapTime ; 
		int minute =  (int) (lapTime / 1000 / 60);  
		lapTime = lapTime - TimeUnit.MINUTES.toMillis(minute);
		int seconds = (int) (this.currentlapTime /1000) ; 
		lapTime  = lapTime - TimeUnit.SECONDS.toMillis(seconds);
		Long ms  = lapTime;
		log.debug(String.format("min:%02d ,sec:%02d ,ms:%02d", 
				minute,seconds, ms	)
			);
		
	}
}
