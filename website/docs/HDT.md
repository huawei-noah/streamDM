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

(add later)

### Hoeffding Adaptive Tree

(add later)



