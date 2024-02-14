package se.kth.ci;

/**
 * Class representing the error codes that can be returned by the CI server.
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 */
public enum ErrorCode {

    /**
     * An error occurred while running shell commands
     */
    ERROR_IO,

    /**
     * An error occurred while cloning the repository
     */
    ERROR_CLONE,

    /**
     * An error occurred while reading the repository
     */
    ERROR_FILE,

    /**
     * An error occurred while building the project
     */
    ERROR_BUILD,

    // An error occured while setting commit status
    ERROR_STATUS,

    // The build was successful
    /**
     * An error occurred while trying to insert a value in db
     */
    ERROR_INSERT_DB,

    /**
     * An error occurred while testing the project
     */
    ERROR_TEST,

    /**
     * The project does not contain any tests
     */
    NO_TESTS,

    /**
     * The method was successful
     */
    SUCCESS
}
