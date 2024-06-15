we have implemented this assignment in two different directories.
PartA_BitcoinAndtransaction
PartB_BitcoinSecurity


PartA_BitcoinAndtransaction:
This part inludes implementation of Bitcoin and performing transactions.(Implementation of Part 3 and 4 mention in assignment doc)

How to run:
run "make"

it will ask for number of nodes you want to run.
On terminal fisrt it will show the states of nodes after genesis block
Then if "wantToprintTxn" (declared in main class) is true it will print all transactions performed in bitcoin chain

"printOnConsole" is true it will print txn log on console as well as in logfiles\MyLogFile.log else only in the file.
Logs can be cleaned by running parseLogs.py available in logfiles folder. It will create a new file "cleandedlog.log"
That have timestamps removed.


PartB_BitcoinSecurity
This part inludes implementation of Part 5: Security: Dishonest nodes of Assignment.
How to run:
run "make"

it will ask for number of all nodes you want to run.
Then it will ask how many dishonest nodes you want to run.

Logfile can be found in logfiles folder.
