---
layout: page
title: Hoeffding Decision Trees
image:
  feature: screen_network.png
---

## Hoeffding Decision Trees

### Hoeffding Tree

Hoeffding tree is an incremental decision tree learner for extreamely large datesets or infinite data stream, assuming that the distribution of data does not change over time. It incrementally grows a decision tree based on the Hedffding bound(or additive Chernoff bound). A node is expanded, as soon as there is sufficient statistical evidence to choose an optimal splitting feature, and the decision is based on the distribution-independent Hoeffding bound. The output of Hoeffding tree is asymptotically nearly identical to that of a non-incremental learner using inﬁnitely datasets. See for details:

*P. Domingos and G. Hulten. Mining High-Speed Data Streams. In KDD'00, pages 71-80, Boston, MA, 2000. ACM Press.

*G. Hulten, L. Spencer, and P. Domingos. Mining time-changing data streams. In KDD’01, pages 97–106, San Francisco, CA, 2001. ACM Press.



Its parameters are as the following:

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



