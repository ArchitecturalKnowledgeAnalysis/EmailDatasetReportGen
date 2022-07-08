# jVisualizer
A program for visualizing the results of analysis over an email dataset. This program is meant to run as the final stage in a pipeline that extracts, analyzes, and visualizes an email dataset. It runs a series of _renderers_ in parallel that read the JSON data produced by analysis, to generate charts.

It takes a single required command-line argument, that being the path to the JSON file to read. All visualizations are generated within the current working directory of the program.
