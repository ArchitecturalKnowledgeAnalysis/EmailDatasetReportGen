import json
import sys
import matplotlib.pyplot as plt
import matplotlib.cm as cm
from matplotlib.colors import Normalize
import numpy as np

# Load JSON data from the file that was specified as the first command-line arg.
analysis_file = open(sys.argv[1], "r")
data = json.load(analysis_file)
analysis_file.close()

my_cmap = cm.get_cmap('jet')

def getColors(choice_count):
    return my_cmap([90 / choice_count * x for x in range(choice_count)])

# Email tag counts.


count = data['count']
emailTagCounts = count['email_tag_counts']
akTypes = {
    'existence': {
        'color': 'blue',
        'data': emailTagCounts['existence']
    },
    'process': {
        'color': 'red',
        'data': emailTagCounts['process']
    },
    'property': {
        'color': 'yellow',
        'data': emailTagCounts['property']
    },
    'technology': 'green'
}
x = list(count['email_tag_counts'].keys())
y = list(count['email_tag_counts'].values())
fig, ax = plt.subplots()
ax.bar(x, y, width=1, edgecolor='white', linewidth=0.7, color=getColors(len(x)))
for lbl in ax.get_xticklabels():
    lbl.set_rotation(45)
    lbl.set_ha('right')
plt.savefig('email_tag_counts.jpeg', bbox_inches='tight')

# Thread tag counts.
x = list(count['thread_tag_counts'].keys())
y = list(count['thread_tag_counts'].values())
fig, ax = plt.subplots()
ax.bar(x, y, width=1, edgecolor='white', linewidth=0.7)
for lbl in ax.get_xticklabels():
    lbl.set_rotation(45)
    lbl.set_ha('right')
plt.savefig('thread_tag_counts.jpeg', bbox_inches='tight')
