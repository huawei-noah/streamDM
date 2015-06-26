---
layout: page
title: Naive Bayes
image:
  feature: screen_network.png
---

## Multinomial Naive Bayes

*Andrew Mccallum and Kamal Nigam.  A comparison of event models for naive bayes
text classification. In AAAI-98 Workshop on ’Learning for Text Categorization’,
1998*

### General Description

Multinomial Naive Bayes models a document as a bag-of-words. For each class
\\(c\\), \\( \Pr(w|c)\\) (the probability of observing word \\(w\\) given
\\(c\\))  is estimated from the training data, simply by computing the relative
frequency of each word in the collection of training documents, for that class.
The classifier also requires the prior probability \\(\Pr(c)\\), which is
straightforward to estimate from the frequency of classes in the training set.

Assuming \\(n_{wd}\\) is the number of times word \\(w\\) occurs in document
\\(d\\), the probability of class \\(c\\) given a test document is calculated as
follows:
$$\Pr(c|d) =\frac{\Pr(c) \prod_{w\in d} \Pr(w|c)^{n_{wd}}}{\Pr(d)},$$
where \\(\Pr(d)\\) is a normalization factor. To avoid the zero frequency
problem, it is common to use the Laplace correction for all conditional
probabilities involved, which means all counts are initialized with a value of 1
instead of 0.

### Implementation

In StreamDM, we have implemented the offline Multinomial Naive Bayes, for use
in the other online algorithms implemented. The model is handled by the
`MultinomialNaiveBayesModel` class, which keeps the class and feature
statistics. The main algorithm is implemented in `MultinomialNaiveBayes`, which
is controlled by the following options:

* Number of features (**-f**), which sets the number of features in the input
  examples (3 by default);
* Number of classes (**-c**), which sets the number of classes in the input
  examples (2 by default); and
* Laplace smoothing (**-s**), which sets the smoothing factor for handling the
  zero frequency issue (1 by default).
