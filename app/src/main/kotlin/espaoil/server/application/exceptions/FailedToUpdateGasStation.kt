package espaoil.server.application.exceptions

class FailedToUpdateGasStation(cause: Throwable) :
    RuntimeException(
        "Failed to update gas stations",
        cause
    )
