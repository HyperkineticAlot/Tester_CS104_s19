#include <iostream>
#include <fstream>
#include "split.h"

using namespace std;

int main(int argc, char* argv[])
{
	if(argc < 2)
	{
		cout << "Please enter input file name." << endl;
		return 1;
	}

	ifstream in(argv[1]);
	if(in.fail())
	{
		cout << "Input file error." << endl;
		return 1;
	}

	int nextVal;
	if(!(in >> nextVal))
	{
		cout << "Empty file or invalid input." << endl;
		return 1;
	}
	
	Node* list = new Node(nextVal, NULL);
	Node* thisElement = list;
	while(in >> nextVal)
	{
		Node* next = new Node(nextVal, NULL);
		thisElement->next = next;
		thisElement = next;
	}

	Node* odds = NULL;
	Node* evens = NULL;
	split(list, odds, evens);

	cout << "Odds:" << endl;
	thisElement = odds;
	while(thisElement != NULL)
	{
		cout << thisElement->value << endl;
		Node* temp = thisElement;
		thisElement = thisElement->next;
		delete temp;
	}

	cout << "Evens:" << endl;
	thisElement = evens;
	while(thisElement != NULL)
	{
		cout << thisElement->value << endl;
		Node* temp = thisElement;
		thisElement = thisElement->next;
		delete temp;
	}

	thisElement = list;
	while(thisElement != NULL)
	{
		Node* temp = thisElement;
		thisElement = thisElement->next;
		delete temp;
	}

	return 0;	
}