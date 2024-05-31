#! /usr/bin/bash

# Script for running a peer
# To be run in the root of the build tree
# No jar files used
# Assumes that Peer is the main class 
#  and that it belongs to the peer package
# Modify as appropriate, so that it can be run 
#  from the root of the compiled tree

# Check number input arguments
argc=$#

if (( argc != 5 )) 
then
	echo "Usage: $0 <access_point> <peer_addr> <peer_port> <boot_addr> <boot_port>"
	exit 1
fi

# Assign input arguments to nicely named variables

sap=$1
peer_addr=$2
peer_port=$3
boot_addr=$4
boot_port=$5

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

# echo "java Peer ${sap} ${peer_addr} ${peer_port} ${boot_addr} ${boot_port}"

java Peer ${sap} ${peer_addr} ${peer_port} ${boot_addr} ${boot_port}
