#! /bin/sh


CITY=Bhubaneswar



while true;
do
	echo "------------------"
	echo "Today's weather condition is ..."

	response=$(curl -s https://wttr.in/${CITY}?format=j1)

        temp=$(echo "$response" | jq -r '.current_condition[0].temp_C')
        weather_condition=$(echo "$response" | jq -r '.current_condition[0].weatherDesc[0].value')

	echo "Today's date: $(date)"
	echo "Present temperature in location ${CITY} is : $temp"
	echo "Present weather condition is : $weather_condition"

	echo "----------------------------------------"

	#sleep 100

done



 
