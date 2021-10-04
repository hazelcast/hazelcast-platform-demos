import gaussian_predict

# Data, Key and 23 features
item1 = 'data,aaaa,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1'

batch1 = []
batch1.append(item1)

def testit(test, batch):
    lenIn = len(batch)
    print("TEST " + str(test) + ":INPUT SIZE " + str(lenIn))
    for item in batch:
        print("TEST " + str(test) + ":INPUT: " + item)
    results = gaussian_predict.predict(batch)
    lenOut = len(results)
    if lenIn != lenOut:
        print("lenIn=" + str(lenIn))
        print("lenOut=" + str(lenOut))
        raise RuntimeError("Input/Output length mismatch")
    print("TEST " + str(test) + ":OUTPUT SIZE " + str(lenOut))
    for result in results:
        print("TEST " + str(test) + ":GAUSSIAN: returned '" + result + "'")

# TESTS
testit(1, batch1)
