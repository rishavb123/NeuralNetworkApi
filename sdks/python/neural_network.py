import requests

class NeuralNetwork:

    def __init__(self, *args):
        if len(args) > 1:
            response = requests.post('http://neuralnetworkapi.bhagat.io/create', json={ 'shape': [x for x in args] })
            self.key = response.json()['key']
        elif len(args) == 1:
            self.key = args[0]

    def connect(key):
        self.key = key

    def train(self, inputs, outputs, epoch):
        if len(inputs) != len(outputs):
            raise Exception('Inputs and Outputs must have the same length')
        data = [{ 'input': inputs[i], 'output': outputs[i] } for i in range(len(inputs))]
        return requests.post('http://neuralnetworkapi.bhagat.io/train', json={ 'key': self.key, 'data': data, 'epoch': epoch }).text

    def predict(self, inp):
        return requests.post('http://neuralnetworkapi.bhagat.io/predict', json={ 'input': inp, 'key': self.key }).json()['output']

    def delete(self):
        response = requests.post('http://neuralnetworkapi.bhagat.io/destroy', json={ 'key': self.key })
        self.key = ''
        return response.text
