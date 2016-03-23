import numpy
import json
import matplotlib.pyplot as plt

json_file = open('data.json','r');
json_data = json_file.read();

JSON_DECODED = json.loads(json_data);

function = numpy.poly1d(numpy.polyfit(JSON_DECODED['trainX'], JSON_DECODED['trainY'], 1))

Q1 = function(JSON_DECODED['qualityX'][0])
Q2 = function(JSON_DECODED['qualityX'][1])

C1 = JSON_DECODED['qualityY'][0]
C2 = JSON_DECODED['qualityY'][1]

Xvals = numpy.arange(120, 255, 1)

plt.plot(JSON_DECODED['trainY'], [255-i for i in JSON_DECODED['trainX']], 'ro', function(Xvals), [255-i for i in Xvals] , 'b')
plt.axis([0, 1500, 0, 255])
plt.ylabel('Average Darkness')
plt.xlabel('Concentraion (ng/mL)')
function_copy = function
function = numpy.poly1d(numpy.array([-1.0/function[1],255+function[0]/function[1]]))
print [function(i) for i in JSON_DECODED['trainY']], [255-i for i in JSON_DECODED['trainX']]
plt.title('%s' % function)


print 'Calculated: ', Q1, ', Actual: ', C1, ', Error: ', abs(Q1-C1)*100/C1
print 'Calculated: ', Q2, ', Actual: ', C2, ', Error: ', abs(Q2-C2)*100/C2

print 'Sample\'s Concentraion: ', function_copy(JSON_DECODED['sample'])
print 'Equation: %s' % function

plt.show()