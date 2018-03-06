package de.adesso.anki.battle.sync;

import de.adesso.anki.battle.util.Position;
import de.adesso.anki.battle.world.World;
import de.adesso.anki.battle.world.bodies.Vehicle;
import de.adesso.anki.roadmap.roadpieces.Roadpiece;
import de.adesso.anki.sdk.AnkiVehicle;
import de.adesso.anki.sdk.messages.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnkiSynchronization {

    @Autowired
    private World world;

    private boolean toggle;

    public void synchronizeState(Vehicle vehicle) {
        if (vehicle.getAnkiReference() != null) {
            val anki = vehicle.getAnkiReference();

            if (vehicle.getTargetSpeed() > 0) {
                val speed = new SetSpeedMessage((int) vehicle.getTargetSpeed() + 1, (int) vehicle.getAcceleration());
                send(anki, speed);
            }

            val speed2 = new SetSpeedMessage((int) vehicle.getTargetSpeed(), (int) vehicle.getAcceleration());
            send(anki, speed2);

            val lane = new ChangeLaneMessage(
                    (float) vehicle.getTargetOffset(),
                    (int) vehicle.getHorizontalSpeed(),
                    1500);
            send(anki, lane);
        }
    }

    private void send(AnkiVehicle anki, Message message) {
        log.debug("< " + message.toString());
        anki.sendMessage(message);
    }

    public void setupHandlers(Vehicle vehicle) {
        if (vehicle.getAnkiReference() != null) {
            val anki = vehicle.getAnkiReference();

            anki.addMessageListener(LocalizationPositionUpdateMessage.class, m -> updatePosition(vehicle, m));
            anki.addMessageListener(LocalizationTransitionUpdateMessage.class, m -> updateTransition(vehicle, m));
            anki.addMessageListener(SpeedUpdateMessage.class, m -> updateSpeed(vehicle, m));
            anki.addMessageListener(OffsetUpdateMessage.class, m -> updateOffset(vehicle, m));
            anki.addMessageListener(Message.class, m -> {
                log.debug("> " + m.toString());
            });
        }
    }

    private void updatePosition(Vehicle vehicle, LocalizationPositionUpdateMessage m) {
        // If position doesn't match internal position
        // set correct roadpiece and set position to null

        /*/
        if (vehicle.getPosition() == null || vehicle.getCurrentRoadpiece() == null) {
            val roadmap = world.getRoadmap();
            Roadpiece piece = roadmap.findRoadpieceByLocation(m.getRoadPieceId(), m.getLocationId(), m.isParsedReverse());

            vehicle.setCurrentRoadpiece(piece);
        }
        /**/

        val ankiPiece = Roadpiece.createFromId(m.getRoadPieceId());
        val segment = ankiPiece.getSegmentByLocation(m.getLocationId(), m.isParsedReverse());
        val offset = segment.getOffsetByLocation(m.getLocationId());
        vehicle.getAnkiReference().sendMessage(new SetOffsetFromRoadCenterMessage((float) offset));
        vehicle.setCalibrationOffset(offset);
    }

    private void updateTransition(Vehicle vehicle, LocalizationTransitionUpdateMessage m) {
        log.info(m.toString());
        if (vehicle.getCurrentRoadpiece() != null) {
            val piece = vehicle.getCurrentRoadpiece();

            /*/
            if (vehicle.getPosition() != null) {
                val maxTravel = piece.findMaximumTravel(vehicle.getPosition());
                val fullTravel = piece.findMaximumTravel(piece.getEntry().transform(Position.at(0, -vehicle.getOffset())));

                if (maxTravel > fullTravel - maxTravel) {
                    log.info("position adjusted: distance=" + vehicle.getPosition().distance(piece.getExit().transform(Position.at(0, -vehicle.getOffset()))));
                    vehicle.setPosition(piece.getExit().transform(Position.at(0, -vehicle.getOffset())));
                } else {
                    log.info("position adjusted: distance=-" + vehicle.getPosition().distance(piece.getEntry().transform(Position.at(0, -vehicle.getOffset()))));
                    vehicle.setPosition(piece.getEntry().transform(Position.at(0, -vehicle.getOffset())));
                }
            } else {
                /**/
            if (vehicle.getPosition() != null)
                log.info("position adjusted: distance=" + vehicle.getPosition().distance(piece.getExit().transform(Position.at(0, -vehicle.getOffset()))));
                vehicle.setPosition(piece.getExit().transform(Position.at(0, -vehicle.getOffset())));
            //}

            vehicle.setCurrentRoadpiece(piece.getNext());
        }
    }

    private void updateSpeed(Vehicle vehicle, SpeedUpdateMessage m) {
        vehicle.setSpeed(m.getLastKnownSpeed());
    }

    private void updateOffset(Vehicle vehicle, OffsetUpdateMessage m) {
        vehicle.setCalibrationOffset(m.getLastKnownOffset());
    }


}