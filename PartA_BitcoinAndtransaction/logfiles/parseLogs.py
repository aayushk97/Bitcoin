file = open("MyLogFile.log", "r")
file2 = open("cleandedlog.log", "w")
levels = ["WARNING", "INFO", "SEVERE", "CONFIG", "FINE", "FINER", "FINEST"]
for line in file:
	if line.startswith(tuple(levels)):
		file2.write(line)
	else:
		file2.write("\n")