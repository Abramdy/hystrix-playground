# hystrix-playground
java playground for hystrix commands, particularly observable command.
 
## AgoObservableCommand

```albgorski.playground.hystrix.AgoObservableCommand``` is an hystrix observable command

It can be created using following parameters: 

* latch: CountDownLatch - used in test to wait until all tasks are done
* workingTimeInMs: long - simulates command working time; how long command should work 
* hystrixTimeOut: int - hystrix command execution timeout in milliseconds 

## Tests
all experiments are placed in the _test_ directory.