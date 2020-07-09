# Environments

Environments are docker images that can be attached to classes and workspaces and are used to compile and run the code contained in a workspace.

The docker containers must contain all of the dependencies required to build the code. Before the container is run all of the workspace files will be copied under the `/workspace` director. The docker container must compile and run the code at the images entry point. `STDOUT` and `STDERR` will be captured from the container and returned in real-time.

An example environment can be found [here](exampleEnv/).