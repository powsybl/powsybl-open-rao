# Modeling counter trade range actions

## Used input data

| Name                    | Symbol           | Details                                                                  |
|-------------------------|------------------|--------------------------------------------------------------------------|
| Upward max value        | $M^{r,+}$        | maximal counter-trade increase in the area of the action                 |
| Downward max value      | $M^{r,-}$        | maximal counter-trade decrease in the area of the action                 |
| Set of generators       | $\mathcal{G}(r)$ | set of generators on the area of a given counter-trade action            |
| max value of generators | $v^i(r)$         | maximal increase or decrease of a generator $i$'s set-point              |
| proportional glsks      | $\lambda_i(r)$   | proportional coefficient of generator $i$                                |
| Capacity of layers      | $m^k(r)$         | maximal counter-trade increase for layer k (see layer definitions below) |



## Defined optimization variables

| Name                            | Symbol            | Details                                                                                                              | Type                | Index                                                         | Unit | Lower bound       | Upper bound       |
|---------------------------------|-------------------|----------------------------------------------------------------------------------------------------------------------|---------------------|---------------------------------------------------------------|------|-------------------|-------------------|
| RA set-point                    | $A(r,s)$          | set-point of RangeAction $r$ (on zone $z$)                                                                           | Real value          | One variable for every element of (CounterTradeRangeActions)  | MW   | Range lower bound | Range upper bound |
| RA set-point upward variation   | $\Delta^{+}(r,s)$ | The upward set-point variation of RangeAction $r$ on state $s$, from set-point on previous state to "RA set-point"   | Real positive value | One variable for every element of (CounterTradeRangeActions)  | MW   | 0                 | $+\infty$         |
| RA set-point downward variation | $\Delta^{-}(r,s)$ | The downward set-point variation of RangeAction $r$ on state $s$, from set-point on previous state to "RA set-point" | Real positive value | One variable for every element of (CounterTradeRangeActions)  | MW   | 0                 | $+\infty$         |
| RA activation                   | $\delta(r,s)$     | Binary variable that indicates whether the range action $r$ is activated at state $s$                                | Binary              | One variable for every element of (CounterTradeRangeActions)  | None | 0                 | 1                 |
| RA layer quantity               | $X_k$             | The amount of counter-trade quantity assigned at layer $k$                                                           | Real positive value | One variable for every layer for each CounterTradeRangeAction | MW   | 0                 | m_k               |
| RA layer activation             | $Y_k$             | Binary variable that indicates whether the layer $k$ is saturated                                                    | Binary              | One variable for every layer for each CounterTradeRangeAction | None | 0                 | 1                 |

## Defined constraints

Counter trade actions are modeled as linear range actions and are defined for each area where they are defined.
We notice that they are only optimized at the preventive state, so for ease of notation the $s$ index for the state will be skipped.
Since counter trading must handle the possibility of generator's saturation, a specific modelization using "layers" is used and presented below.

A counter trade exchange on an area is the net sum of the exchanges on all of its borders. Hence, for each zone, the value $A(r)$ is the sum of the values of all its borders:

$$
\begin{equation}
A(r) = \sum_{b \in Imp(r)} A(b) - \sum_{b \in Exp(r)} A(b).
\end{equation}
$$
where $Imp(r)$ and $Exp(r)$ respectively indicate the range actions on the neighbouring zones from which there are imports or exports of counter-trade.
As a consequence, the set-point increases sum to zero over all zones, which serves as a balance constraint for the countertrading actions.

The variables $A(r)$ can be either positive or negative. Since the counter trading value will be dispatched differently to the generators depending on its direction, we will split it into positive and negative parts:
let $\Delta^+(r)\geq 0$ and $\Delta^-(r)\geq 0$ and:

$$
\begin{equation}
A(r) = \Delta^+(r) - \Delta^-(r)
\end{equation}
$$

A condition ensuring that at most one of the two variables is non-zero must be added, as follows:
$$
\begin{gather}
\Delta^+(r) \leq \delta(r)\, M^{r,+}\\
\Delta^-(r) \leq (1-\delta(r) )\, M^{r,-}
\end{gather}
$$

The two directions of the CT action are modelled in the same way and only differ by their data.


*Generators.* 

The maximal upward and downward variations of generator $i$ from its initial set-point are
$$
\begin{equation}
v_i^+ := max_P^i - g_{init}^i, \qquad v_i^- := g_{init}^i - min_P^i.
\end{equation}
$$

Let $A(i) \geq 0$ be the volume dispatched to generator $i$ in direction $d$: an increase of its set-point for $d = +$, a decrease for $d = -$. Then:

$$
\begin{gather}
\Delta^d(r) = \sum_{i \in \mathcal{G}(z)} A^d(i),\\
0 \leq A^d(i) \leq v^d_i.
\end{gather}
$$

Generator $i$ is **saturated** when $A^d(i) = v^d_i$.

From now on, all quantities refer to a single action $r$ and a single direction $d \in \{+,-\}$,
and the index $r$ and the superscript $d$ are omitted:
$v_i$, $m_k$, $X_k$, $Y_k$ stand for $v_i^d$, $m_k^d$, $X_k^d$, $Y_k^d$.
The superscript is only kept in $\Delta^d(r)$, which links the action variables above to the single-direction model,
and in the few equations where both directions appear.

In the layered model below, $A(i)$ is not a decision variable: it is a linear expression of the layer variables, and the bound on the generator's minimal and maximal variations is implied by the bounds of the layers.

#### Proportional GLSKs

The GLSK consists of a list of proportional coefficients $\lambda_i$ for the generators of the zone, such that $\sum_i \lambda_i = 1$.
Generators with $\lambda_i = 0$ receive no CT volume and are removed from $\mathcal{G}(z)$ beforehand, so that $\lambda_i > 0$ for all $i \in \mathcal{G}(z)$.

The generators are sorted by ascending $v_i/\lambda_i$, and layer $k$ is associated with generator $k$: with this ordering, generator $k$ cannot be saturated unless generator $k-1$ is saturated as well.
The first layer corresponds to the quantity that can be assigned proportionally to all generators, without saturating any of them. Each successive layer $k$ corresponds to the additional quantity of CT that can be assigned to generators $k, \dots, n$, given that all generators $1, \dots, k-1$ are saturated with the previous layers.

When a volume is dispatched proportionally to generators $k,\dots,n$ only, the coefficients applied must sum to 1 and therefore need to be renormalized. For each $k = 1,\dots,n$, we define:

$$
\begin{equation}
\lambda_i^k := \frac{\lambda_i}{\sum_{h=k}^n \lambda_h}\quad\forall i= k, \dots, n, \qquad \lambda_i^k := 0 \quad \forall i < k
\end{equation}
$$

the (renormalized) GLSK coefficient of generator $i$ given that generator $k-1$ is the last one that was saturated.


The value $m_1$ is the largest CT volume that can be dispatched in the zone before generator 1 saturates, hence $v_1 = \lambda_1 m_1$. For the subsequent generators, both the volume already dispatched and the renormalization must be taken into account. The following formula thus must hold for all $k=1,\dots, n$:
$$
\begin{equation}
v_k = \sum_{h=1}^k m_h \lambda_k^h,
\end{equation}
$$
from which the values $m_k$ are computed iteratively:
$$
\begin{equation}
m_k =  \frac{v_k - \sum_{h=1}^{k-1} m_h \lambda_k^h}{\lambda_k^k}.
\end{equation}
$$
The last equation is the requirement $\sum_k \lambda_i^k m_k \leq v_i$ mentioned above, satisfied with equality.
The total available capacity equals the sum of the maximal volumes:
$$
\begin{align}
\sum_{k=1}^n v_k &= \sum_{k=1}^n \sum_{h=1}^k m_h \lambda_k^h
= \sum_{h=1}^n \sum_{k=h}^n m_h \frac{\lambda_k}{\sum_{l=h}^n \lambda_l}
= \sum_{h=1}^n m_h.
\end{align}
$$


### Layered dispatch
For each zone and each direction, the GLSK is turned into an ordered list of $L$ layers. Layer $k$ has a capacity $m_k \geq 0$ and spreads its volume over the generators with coefficients $\lambda_i^k \geq 0$ such that $\sum_{i} \lambda_i^k = 1$. Let $\Lambda := (\lambda_i^k) \in \mathbb{R}_+^{n \times L}$. The layers are ordered so that a layer can only be used once all the previous ones are full.

Let $X_k \geq 0$ be the CT volume of layer $k$, and $Y_k$ a binary variable equal to 1 when layer $k$ is full. The zonal CT volume and the dispatch to the generators read:
$$
\begin{gather}
\Delta^d(r) = \sum_{k=1}^{L} X_k,\label{zone_x_link}\\
A(i) = \sum_{k=1}^{L} \lambda_i^k X_k, \quad\text{i.e.}\quad A = \Lambda X.
\end{gather}
$$

The order of the layers is enforced by:
$$
\begin{align}
0 \leq X_k & \leq m_k, & \forall k&=1,\dots,L \label{x_bounds}\\
Y_k & \quad \text{is binary}, & \forall k&=1,\dots,L \label{y_binary}\\
Y_k & \geq Y_{k+1}, & \forall k&=1,\dots,L-1 \label{y_nonincreasing}\\
X_{k+1} & \leq Y_k\, m_{k+1}, & \forall k&=1,\dots,L-1 \label{x_saturation_order}\\
X_k & \geq Y_k\, m_k, & \forall k&=1,\dots,L. \label{x_saturation}
\end{align}
$$
These constraints force the $Y$ variables to be non-increasing, $X_{k+1}$ to 0 whenever layer $k$ is not full, and $X_k$ to its maximal value whenever $Y_k = 1$.

Each GLSK type must provide the layers such that $\sum_{k} \lambda_i^k m_k \leq v_i$ for every generator $i$.
The bounds on the X variables then imply the generator bounds, which do not need to be written in the model.
Since the columns of $\Lambda$ sum up to 1, $\sum_i A(i) = \sum_k X_k$, and the CT volume reachable in a direction is bounded by $\min\left(\alpha_{MAX}^{z,d}, \sum_{k=1}^{L} m_k\right)$.

#### Reachable layers

With many layers and a limited CT volume, some layers can never be filled inside the allowed range.
For each direction, let $K$ be the first index such that
$\sum_{h\leq K}m_h \geq M^{r,d}$, where $M^{r,d}$ is the maximum CT volume of the zone in direction $d$. If no such index exists, $K = |\mathcal{G}|$. Then:

- layers $1, \dots, K-1$ keep their variables $X_k$ and $Y_k$;
- layer $K$ keeps its variable $X_K$, bounded by $\min(m_K, M^{r,d})$, but $Y_K$ is fixed to 0, since layer $K$ cannot be full before $M^{r,d}$ is reached;
- layers $K+1, \dots, L$ are removed: $X_k = 0$ and $Y_k = 0$.

In the proportional case, layer $K$ spreads its volume over generators $K, \dots, n$, which form the so-called **tail**: a set of generators, none of which can saturate.

Thus, for each area and each direction, the model contains $K$ continuous variables and $K-1$ binary variables.









