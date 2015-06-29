---
layout: page
title: Stochastic Gradient Descent
image:
  feature: screen_network.png
---

## SGD Learner

The `SGDLearner` implements the _stochastic gradient descent optimizer_ for
learning various linear models: binary class SVM, binary class logistic
regression, and linear regression.

Its parameters are the following:

* Loss function parameter (**-o**), can be `LogisticLoss` (for logistic
  regression), `Squaredloss` (for linear regression, default), `HingeLoss` (for
  SVM), and `PerceptronLoss` (for perceptron);
* Regularizer parameter (**-r**), can be `ZeroRegularizer` (no regularization,
  default), `L1Regularizer`, and `L2Regularizer`;
* Regularization parameter (**-p**), which controls the influence of the
  regularization; and
* Learning parameter (**-l**), which controls the rate of update for the
  gradient descent.

For example, to instruct `EvaluatePrequential` to use logistic regression as a
classifier, we only need to pass `LogisticLoss` as the loss function:

```bash
EvaluatePrequential -l (SGDLearner -o LogisticRegression -r ZeroRegularizer -p .001 -l .001)
```

### <a name="perceptron"></a>Perceptron

The `Perceptron` is a `SGDLearner` with `PerceptronLoss` by default; the other
options remain the same as in the `SGDLearner`. 
