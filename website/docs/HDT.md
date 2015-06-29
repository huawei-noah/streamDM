---
layout: page
title: Hoeffding Decision Trees
image:
  feature: screen_network.png
---

## Hoeffding Decision Trees

### General Description

The Hoeffding tree is an incremental decision tree learner for large data
streams, that assumes that the data distribution is not changing over time. It
grows incrementally a decision tree based on the theoretical guarantees of the
Hoeffding bound (or additive Chernoff bound). A node is expanded as soon as
there is sufficient statistical evidence that an optimal splitting feature
exists, a decision based on the distribution-independent Hoeffding bound. The
model learned by the Hoeffding tree is asymptotically nearly identical to the
one built by a non-incremental learner, if the number of training instances is
large enough. See for details:

*P. Domingos and G. Hulten. Mining High-Speed Data Streams. In KDD, pages 71-80,
Boston, MA, 2000. ACM Press.*

*G. Hulten, L. Spencer, and P. Domingos. Mining time-changing data streams. In
KDD, pages 97–106, San Francisco, CA, 2001. ACM Press.*

### Implementation

In StreamDM, Hoeffding trees are implemented in the `HoeffdingTree` class,
supported by the model implemented in `HoeffdingTreeModel`. The `HoeffdingTree`
is controlled by the following parameters:

* Numeric observer to use (**-n**); for the moment, only Gaussian approximation is supported; class of type `FeatureClassObserver`;
* Number of examples a leaf should observe before a split attempt (**-g**);
* Number of examples a leaf should observe before applying NaiveBayes (**-q**);
* Split criterion to use (**-s**), an object of type `SplitCriterionType`;
* Allowable error in split decision (**-c**);
* Threshold of allowable error in breaking ties (**-t**);
* Allow only binary splits (**-b**), boolean flag;
* Disable poor attributes (**-r**);
* Allow growth (**-o**);
* Disable pre-pruning (**-p**);
* Leaf prediction to use (**-l**), either `MajorityClass` (0), `NaiveBayes` (1)
  or adaptive `NaiveBayes` (2, default); and
* Enable splitting at all leaves (**-a**); the original algorithm can split at
  any time, but in Spark Streaming one can only split once per RDD; the option
  controls whether to split at leaf of the last `Example` of the RDD, or at
  every leaf.
