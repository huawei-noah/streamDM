---
layout: page
title: Hoeffding Decision Trees
image:
  feature: screen_network.png
---

## Hoeffding Decision Trees

### Hoeffding Tree

A Hoeffding tree is an incremental, anytime decision tree induction algorithm that is capable of learning from massive data streams, assuming that the distribution generating examples does not change over time. Hoeffding trees exploit the fact that a small sample can often be enough to choose an optimal splitting attribute. This idea is supported mathematically by the Hoeffding bound, which quantiﬁes the number of observations (in our case, examples) needed to estimate some statistics within a prescribed precision (in our case, the goodness of a feature). A theoretically appealing feature of Hoeffding Trees not shared by other incremental decision tree learners is that it has sound guarantees of performance. Using the Hoeffding bound one can show that its output is asymptotically nearly identical to that of a non-incremental learner using inﬁnitely many examples. See for details:

*G. Hulten, L. Spencer, and P. Domingos. Mining time-changing data streams. In KDD’01, pages 97–106, San Francisco, CA, 2001. ACM Press.



Its parameters are the following:

-n : Numeric Observer to use : only support Gaussian approximation by now. And 0 for GuassianNumericFeatureClassObserver,and default number of bins is 10.
-g : The number of examples a leaf should observe between split attempts
-s : Split criterion to use. Example : InfoGainSplitCriterion
-c : The allowable error in split decision, values closer to 0 will take longer to decide
-t : Threshold below which a split will be forced to break ties
-b : Only allow binary splits
-r : Disable poor attributes
-o : Growth allowed
-p : Disable pre-pruning
-l : Leaf prediction to use: MajorityClass (0), Naive Bayes (1) or NaiveBayes adaptive (2).Default is NaiveBayes adaptive.
-q : The number of examples a leaf should observe before permitting Naive Bayes
-a :Split at all leaves. Different from origin algorithm, which can split at any time, Spark streaming version can only split once for each RDD, Either split at the leaf the last Example of one RDD belonged to, or try to split at all leaves.

### Hoeffding Adaptive Tree

(add later)



