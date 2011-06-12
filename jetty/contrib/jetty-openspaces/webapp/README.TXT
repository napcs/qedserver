
To build:

download and unpack gigaspace release (6.5rc2).
Run gigaspace script to populate your local repository

mvn clean compile package



To run:

you need to start (from $GIGA_HOME/bin):

gsm.sh     (the space manager)
gs-ui.sh   (the space user interface)
gsc.sh * 2 (two space containers)


then run 

mvn os:deploy

it will find the gsm and deploy two instances of the webapp (currently controlled by sla bean in pu.xml)
The server is also defined in pu.xml

