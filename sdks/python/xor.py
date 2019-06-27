from neural_network import NeuralNetwork

nn = NeuralNetwork(2, 4, 1)
inputs = [
    [1, 0],
    [0, 1],
    [1, 1],
    [0, 0]
]
outputs = [
    [1],
    [1],
    [0],
    [0]
]
epoch = 100000
print(nn.train(inputs, outputs, epoch))
print(nn.predict([1, 0]))
print(nn.delete())