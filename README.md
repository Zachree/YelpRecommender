# YelpRecommender
Spring 2021

This program uses a GUI to list 10 businesses from a dataset of some thousands of businesses, where any of them may be selected to display 5 "similar" businesses. These 10 can be refreshed. Additionally, the business-similarity clusters can be generated - displaying the medoid center-point for all 5 clusters. Selecting a medoid displays the business geographically nearest to the medoid and all of the businesses in its cluster.

Notable utilities: custom BTree implementation, custom hash table implementation, TF-IDF and Levenshtein edit distance for calculating similarity, k-medoids for clustering, Haversine formula for calculating geographic distance.

UI:

![image](https://user-images.githubusercontent.com/97318794/175034671-7f62b887-1a34-473f-aad1-949ee33e7235.png)

Recommended Businesses (Panera Bread selected):

![image](https://user-images.githubusercontent.com/97318794/175035599-6529c006-01a7-49c0-8836-5174b3d9beba.png)

Single entire cluster:

![image](https://user-images.githubusercontent.com/97318794/175035068-31c3a619-2c3a-4d48-9696-434f3b15d782.png)
