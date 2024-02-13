package se.kth.ci;

/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public enum ErrorCode {
    // An error occurred while running shell commands
    ERROR_IO,

    // An error occurred while cloning the repository
    ERROR_CLONE,

    // An error occurred while reading the repository
    ERROR_FILE,

    // An error occurred while building the project
    ERROR_BUILD,

    // An error occured while trying to insert a value in db
    ERROR_INSERT_DB,

    // The build was successful
    SUCCESS,

}
