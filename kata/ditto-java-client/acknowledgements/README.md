# Ditto Java Client - Acknowledgements

In this exercise, both 
* requesting acknowledgements and
* emitting acknowledgements after an event was received
is practiced.  

Fill in your credentials in `src/test/resources/config.properties`.

The Katas are provided as JUnit tests.
Find the Katas in folder `src/main/test`, replace the TODOs with actual code and run the test.

****
* `Kata1:` While subscribing to attribute changes:
    * acknowledge each "even" attribute counter value with a successful custom ack,
    * acknowledge each "odd" attribute counter value change with a non-successful custom ack.
* `Kata2:` Perform an attribute modification requesting 2 acknowledgements and check why the custom ack failed.