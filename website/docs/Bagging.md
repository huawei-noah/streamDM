---
layout: page
title: Bagging
image:
  feature: screen_network.png
---

## Online Bagging

*Nikunj C. Oza, Stuart J. Russell:
Online Bagging and Boosting. AISTATS 2001*

###General Description


*Bagging* is an ensemble method that improves the accuracy of a single classifier. 
Non-streaming bagging builds a set of base models, training each model with a 
bootstrap sample created by drawing random samples with replacement from the 
original training set.  Each base model's training set contains each of the 
original training example a number of times that follows a binomial distribution. 
This binomial distribution  for large values tends to a Poisson(1) distribution. 
This Online Bagging classifier, instead of using sampling with replacement, 
it gives each example a weight according to Poisson(1). 

###Implementation

In StreamDM, the main algorithm is implemented in `Bagging`, which
is controlled by the following options:

* Number of classifiers (**-s**), which sets the number of classifiers used to build the
  ensemble (10 by default); and
* Base classifier (**-l**), which sets the base classifier to be used to build the members 
  of the ensemble (SGDLearner by default).
