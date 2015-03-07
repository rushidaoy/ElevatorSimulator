package elevatorsimulator;
import java.util.LinkedList;
import java.util.Queue;

import elevatorsimulator.ElevatorCar.State;

/**
 * Represents a floor in the building
 * @author Anton Jansson
 *
 */
public class Floor {
	private final int floorNumber;
	private final int numResidents;
	private final double averageArrivalRate;
	private final Queue<Passenger> waitingQueue;
	
	private long timeLeft = 0;
	private boolean isFirst = true;
	
//	private long lastIntervalStart = 0;
		
	/**
	 * Creates a new floor
	 * @param floorNumber The floor number
	 * @param numResidents The number of residents
	 * @param averageArrivalRate The average arrival rate
	 */
	public Floor(int floorNumber, int numResidents, double averageArrivalRate) {
		this.floorNumber = floorNumber;
		this.numResidents = numResidents;
		this.averageArrivalRate = averageArrivalRate;
		this.waitingQueue = new LinkedList<Passenger>();
	}
	
	/**
	 * Returns the floor number
	 */
	public int getFloorNumber() {
		return floorNumber;
	}

	/**
	 * Returns the number residents
	 */
	public int getNumResidents() {
		return numResidents;
	}

	/**
	 * Returns the average arrival rate
	 * @return
	 */
	public double getAverageArrivalRate() {
		return averageArrivalRate;
	}
	
	/**
	 * Returns the waiting queue for the floor
	 */
	public Queue<Passenger> getWaitingQueue() {
		return waitingQueue;
	}
	
	private void generateNextArrival(Simulator simulator) {
//		double nextTime = (-Math.log(1.0 - simulator.getRandom().nextDouble()) * (this.averageArrivalRate));
//		this.timeLeft = simulator.getClock().secondsToTime(nextTime);
		this.timeLeft = simulator.getClock().minutesToTime(this.averageArrivalRate);
	}
	
	/**
	 * Generates a random destination floor
	 * @param simulator The simulator
	 */
	private int generateRandomDestination(Simulator simulator) {
		while (true) {
			int randFloor = simulator.getRandom().nextInt(simulator.getBuilding().numFloors());
			
			if (randFloor != this.floorNumber) {
				return randFloor;
			}
		}
	}
	
	/**
	 * Marks that the given hall call has been handled
	 * @param simulator The simulator
	 * @param passenger The passenger
	 */
	private void hallCallHandled(Simulator simulator, Passenger passenger) {
		this.waitingQueue.remove(passenger);
		simulator.getControlSystem().hallCallHandled(passenger);
	}
	
	/**
	 * Updates the floor
	 * @param simulator The simulator
	 * @param duration The elapsed time since the last time step
	 */
	public void update(Simulator simulator, long duration) {
		if (!this.waitingQueue.isEmpty()) {
			for (Passenger passenger : new LinkedList<Passenger>(this.waitingQueue)) {
				for (ElevatorCar elevator : simulator.getBuilding().getElevatorCars()) {
					if (elevator.getState() == State.STOPPED || elevator.getState() == State.IDLE) {
						//Check if the elevator has capacity
						if (!elevator.canPickupPassenger(passenger)) {
							continue;
						}
						
						boolean canPickup = false;
						
						Direction dir = Direction.getDirection(this.floorNumber, passenger.getDestinationFloor());
						
						if (this.floorNumber == elevator.getFloor()) {
							canPickup = 
								elevator.getDirection() == Direction.NONE
								|| elevator.getDirection() == dir;
						}
						
						
						if (canPickup) {
							simulator.elevatorLog(elevator.getId(), "Picked up passenger #" + passenger.getId() + " at floor "
								+ this.floorNumber + " with the destination of "
								+ passenger.getDestinationFloor() + ".");
								
							elevator.setDirection(dir);
							elevator.pickUp(simulator, passenger);
							this.hallCallHandled(simulator, passenger);
							break;
						}
					}
				}
			}
		}
				
		this.tryGenerateNewArrival(simulator, duration);
	}
	
	/**
	 * Tries to generate a new arrival on the floor. The success of the method depends on the probability.
	 * @param simulator The simulator
	 * @param duration The elapsed time since the last time step
	 * @return True if generated
	 */
	public boolean tryGenerateNewArrival(Simulator simulator, long duration) {
		if (this.isFirst) {
			this.generateNextArrival(simulator);
			this.isFirst = false;
			return false;
		}
		
		this.timeLeft -= duration;
		
		if (this.timeLeft <= 0) {
			int randFloor = generateRandomDestination(simulator);
			
			Passenger newPassenger = new Passenger(
				simulator.nextPassengerId(),
				this.floorNumber,
				randFloor,
				1,
				simulator.getClock());
			
			this.waitingQueue.add(newPassenger);		
			simulator.getControlSystem().handleHallCall(newPassenger);
			
//			simulator.log(
//					"Generated a new passenger at floor "
//					+ this.floorNumber + " with the destination: "
//					+ newPassenger.getDestinationFloor() + ".");
			
			simulator.arrivalGenerated(this.floorNumber);
			
			this.generateNextArrival(simulator);
			return true;
		}
			
		return false;
	}
}
