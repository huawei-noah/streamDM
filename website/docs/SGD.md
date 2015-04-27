---
layout: page
title: Stochastic Gradient Descent
image:
  feature: screen_network.png
---

## SGD Learner

The SGDLearner implements stochastic gradient descent for learning various
linear models: binary class SVM, binary class logistic regression
and linear regression.

The parameters needed are the following

* Loss function to use (-o)
 * LogisticLoss
 * PerceptronLoss
 * HingeLoss
 * SquaredLoss
* Regularizer to use (-r)
 * ZeroRegularizer
 * L1Regularizer
 * L2Regularizer 
* Regularization parameter (-p)
* Lambda parameter (-l)

To use Logistic Regression as a classifier we only need to specify LogisticLoss as the loss function:

{% highlight bash %}
 -l (SGDLearner -o LogisticRegression -r ZeroRegularizer -p .001 -l .001)
{% endhighlight %}


## <a name="perceptron"></a>Perceptron

The Perceptron is a SGDLearner with PerceptronLoss. 

 
