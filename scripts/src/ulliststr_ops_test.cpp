#include "ulliststr.h"
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>

using namespace std;

bool checkErr(const stringstream &ss, string command, bool illegal);
void deallocate(ULListStr** lists, int curr);

int main(int argc, char* argv[])
{
	if(argc < 2)
	{
		cout << "Include an input file name" << endl;
		return 1;
	}

	ifstream in(argv[1]);
	if(in.fail())
	{
		cout << "Error opening input stream" << endl;
		return 1;
	}

	int count;
	int curr = -1;
	in >> count;
	ULListStr** lists = new ULListStr*[count];

	string thisLine;
	while(!in.eof() && !in.fail())
	{
		getline(in, thisLine);
		if(thisLine == "") continue;
		stringstream ss(thisLine);

		string token;
		ss >> token;
		if(token == "list")
		{
			if(curr > count - 1)
			{
				cout << "Number of lists exceeds provided count " 
					 << count << endl;
				deallocate(lists, curr);
				return 1;
			}
			
			ULListStr* newList = new ULListStr();
			string nextVal;
			while(ss >> nextVal)
			{
				newList->push_back(nextVal);
			}

			lists[++curr] = newList;
		}
		else if(token == "copy")
		{
			int first, second;
			ss >> first >> second;
			if(!checkErr(ss, token, first > curr || second > curr))
			{
				deallocate(lists, curr);
				return 1;
			}

			ULListStr* newList = new ULListStr(*lists[first]);
			delete lists[second];
			lists[second] = newList;
		}
		else if(token == "assign")
		{
			int first, second;
			ss >> first >> second;
			if(!checkErr(ss, token, first > curr || second > curr))
			{
				deallocate(lists, curr);
				return 1;
			}

			*lists[second] = *lists[first];
		}
		else if(token == "concat")
		{
			int first, second, target;
			ss >> first >> second >> target;
			if(!checkErr(ss, token, first > curr || second > curr 
				|| target > curr))
			{
				deallocate(lists, curr);
				return 1;
			}

			*lists[target] = *lists[first] + *lists[second];
		}
		else if(token == "minuseq")
		{
			int target, minus;
			ss >> target >> minus;
			bool illegal = target > curr;
			if(!checkErr(ss, token, illegal))
			{
				deallocate(lists, curr);
				return 1;
			}

			*lists[target] -= minus;
		}
		else if(token == "printat")
		{
			int target, index;
			ss >> target >> index;
			bool illegal = target > curr;
			if(!illegal) illegal = index > (int)(lists[target]->size()-1);
			if(!checkErr(ss, token, 
				target > curr || illegal)) 
			{
				deallocate(lists, curr);
				return 1;
			}

			string stringAt = (*lists[target])[index];
			cout << "List " << target << " at " << index 
				 << ": " << stringAt << endl;
		}
		else if(token == "print")
		{
			int target;
			ss >> target;
			if(!checkErr(ss, token, target > curr))
			{
				deallocate(lists, curr);
				return 1;
			}

			cout << "List " << target << ": ";
			for(size_t i = 0; i < lists[target]->size(); i++)
			{
				cout << lists[target]->get(i) << " ";
			}
			cout << endl;
		}
		else
		{
			cout << "Unrecognized token" << endl;
		}
	}
	deallocate(lists, curr);
	return 0;
}

bool checkErr(const stringstream &ss, string command, bool illegal)
{
	if(ss.fail())
	{
		cout << "Insufficient " << command << " arguments" << endl;
		return false;
	}
	if(illegal)
	{
		cout << "Illegal index in " << command << " command" << endl;
		return false;
	}
	return true;
}

void deallocate(ULListStr** lists, int curr)
{
	for(int i = 0; i <= curr; i++)
	{
		delete lists[i];
	}
	delete[] lists;
}
