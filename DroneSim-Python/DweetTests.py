__author__ = 'V646078'

import urllib
import urllib2
# Import pyDweep
import dweet
# Import Dweepy
import dweepy

# Dweet as an object called 'python test', with 'hello world'
url = 'https://dweet.io/dweet/for/python_test'
values = {
    'content': 'Hello World!'
}

# Encode the dweet content
data = urllib.urlencode(values)

# Make the post request
req = urllib2.Request(url, data)

# Acquire the response from Dweet.io
response = urllib2.urlopen(req)

# Get the content of the response and print it
the_page = response.read()
print the_page

# Test pyDweep
dweeter = dweet.Dweet()
dweeter.dweet_by_name("PyDweep_Test", {'Content': 'Success!'})

# Test Dweepy
dweepy.dweet_for("Dweepy_Test", {'Content': 'It Worked!'})