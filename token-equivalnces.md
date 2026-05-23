# Tire pressures
These vary by game, here are the values, and on the csv they are list always as front, then rear:
## F1 22
fronts: min=22.5, max=25.0
rears: min=20.5, max=23.0

## F1 23
fronts: min=22.0, max=25.0
rears: min=20.0, max=23.0

## F1 24
fronts: min=22.5, max=29.5
rears: min=20.5, max=26.5

## F1 25
Same as F1 24.

# Suspension geometry
Here the tokens "L" and "R" refer to the slider being all the way to the left or right, so essentially it means min and max as well. For this section, we have have variations per game as well.
Here, is the same for the tyre pressures, the values are listed as front, then rear, for both the cambers and toe.
So when you see: RRLL, it means the front camber is max value, the rear camber is max value, the front toe is min, and the rear toe is min
And when you see: LL.05L it means the front camber is min value, the rear camber is min value, the front toe is 0.05, and the rear toe is min.

## F1 22
### Front camber
L(min): -3.5, R(max): -2.5
### Rear camber
L(min): -2.0, R(max): -1.0
### Front toe
L(min): 0.05, R(max): 0.15
### Rear toe
L(min): 0.20, R(max): 0.50

## F1 23
### Front camber
L(min): -3.5, R(max): -2.5
### Rear camber
L(min): -2.0, R(max): -1.0
### Front toe
L(min): 0.00, R(max): 0.10
### Rear toe
L(min): 0.10, R(max): 0.30

## F1 24
### Front camber
L(min): -3.5, R(max): -2.5
### Rear camber
L(min): -2.2, R(max): -0.70
### Front toe
L(min): 0.00, R(max): 0.50
### Rear toe
L(min): 0.00, R(max): 0.50

## F1 25
### Front camber
L(min): -3.5, R(max): -2.5
### Rear camber
L(min): -2.0, R(max): -1.0
### Front toe
L(min): 0.00, R(max): 0.20
### Rear toe
L(min): 0.10, R(max): 0.25

# Special tokens
"same" and "same as Q" are the same, it means, that the race tire pressures are the same as the qualifying ones.
"min all" and "max all" mean that all the values for that section are either the minimum or maximum, for both front and rear. if you see "min - 21" means min pressures for fronts and 21 for the rears, and the same applies for max.
the character "/" is sometimes used to separate the wings configuration for the race and qualifying. It has an "r" and "q" to know which side is for which session. You can also find that a parenthesis is used for this same purpose, again the same distinction is made using the letters "q" and "r".
regarding the "*" character, just ignore it as it's used in the spreadsheet to add annotations.
