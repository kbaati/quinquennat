package fr.quinquennat.backend.simulation;

/** The submitted programme references an unknown lever or a value outside its bounds. */
public class InvalidProgramException extends RuntimeException {

    public InvalidProgramException(String message) {
        super(message);
    }
}
