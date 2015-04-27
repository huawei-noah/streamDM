---
layout: page
title: Naive Bayes
image:
  feature: screen_network.png
---

## Naive Bayes Multinomial

Multinomial naive Bayes considers a document as a bag-of-words. For
each class \\(c\\), \\( \Pr(w|c)\\), the probability of observing word \\(w\\) given
this class, is estimated from the training data, simply by computing
the relative frequency of each word in the collection of training
documents of that class. The classifier also requires the prior
probability \\(\Pr(c)\\), which is straightforward to estimate. 

Assuming \\(n_{wd}\\) is the number of times word \\(w\\) occurs in document
\\(d\\), the probability of class \\(c\\) given a test document is calculated
as follows:
$$\Pr(c|d) =\frac{\Pr(c) \prod_{w\in d} \Pr(w|c)^{n_{wd}}}{\Pr(d)},$$
where \\(\Pr(d)\\) is a normalization factor. To avoid the zero-frequency
problem, it is common to use the Laplace correction for all
conditional probabilities involved, which means all counts are
initialized to the 
value one instead of zero.

*Andrew Mccallum and Kamal Nigam.
A comparison of event models for naive bayes text classification. In AAAI-98
Workshop on ’Learning for Text Categorization’, 1998*



