package rocket_bucket

import (
	"fmt"
	"log"
)

func Info(format string, args ...interface{}) {
	doLog("Info", format, args...)
}

func Error(format string, args ...interface{}) {
	doLog("Error", format, args...)
}

func Fatal(format string, args ...interface{}) {
	doLog("Fatal", format, args...)
	log.Panicf(format, args...)
}

func doLog(level string, format string, args ...interface{}) {
	log.Printf(fmt.Sprintf("%s: %s", level, format), args...)
}
