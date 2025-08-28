"""
Behavioral Biometrics ML Module
Advanced machine learning algorithms for behavioral anomaly detection
Pure Python implementation for Chaquopy compatibility
"""

import json
import random
import math
from typing import List, Dict, Tuple, Optional


class IsolationForestSimple:
    """Simplified Isolation Forest implementation for Android/Chaquopy"""
    
    def __init__(self, n_estimators: int = 100, max_samples: int = 256, contamination: float = 0.1):
        self.n_estimators = n_estimators
        self.max_samples = max_samples
        self.contamination = contamination
        self.trees = []
        self.fitted = False
        self.threshold = 0.0
    
    def fit(self, X: List[List[float]]) -> None:
        """Train the isolation forest on baseline data"""
        if not X or not X[0]:
            return
        
        n_samples = len(X)
        n_features = len(X[0]) if X else 0
        
        # Build isolation trees
        self.trees = []
        for _ in range(self.n_estimators):
            # Sample subset of data
            sample_size = min(self.max_samples, n_samples)
            indices = random.sample(range(n_samples), min(sample_size, n_samples))
            sample = [X[i] for i in indices]
            
            # Build tree
            tree = self._build_tree(sample, 0)
            self.trees.append(tree)
        
        # Calculate anomaly scores for training data to set threshold
        scores = []
        for sample in X:
            score = self._anomaly_score(sample)
            scores.append(score)
        
        # Set threshold based on contamination rate
        scores_sorted = sorted(scores, reverse=True)
        threshold_idx = int(len(scores_sorted) * self.contamination)
        self.threshold = scores_sorted[threshold_idx] if threshold_idx < len(scores_sorted) else scores_sorted[-1]
        
        self.fitted = True
    
    def predict(self, X: List[float]) -> Tuple[float, bool]:
        """Predict anomaly score and whether sample is anomalous"""
        if not self.fitted:
            return 0.5, False
        
        score = self._anomaly_score(X)
        is_anomalous = score > self.threshold
        
        # Convert to 0-1 probability
        probability = min(max(score, 0.0), 1.0)
        return probability, is_anomalous
    
    def _build_tree(self, X: List[List[float]], depth: int, max_depth: int = 10) -> Dict:
        """Build a single isolation tree"""
        if len(X) <= 1 or depth >= max_depth:
            return {'type': 'leaf', 'size': len(X)}
        
        # Random feature and split point
        n_features = len(X[0]) if X else 0
        feature = random.randint(0, n_features - 1) if n_features > 0 else 0
        feature_values = [row[feature] for row in X]
        
        if len(set(feature_values)) == 1:
            return {'type': 'leaf', 'size': len(X)}
        
        split_value = random.uniform(min(feature_values), max(feature_values))
        
        # Split data
        left_data = [X[i] for i, val in enumerate(feature_values) if val < split_value]
        right_data = [X[i] for i, val in enumerate(feature_values) if val >= split_value]
        
        return {
            'type': 'split',
            'feature': feature,
            'split_value': split_value,
            'left': self._build_tree(left_data, depth + 1, max_depth),
            'right': self._build_tree(right_data, depth + 1, max_depth)
        }
    
    def _path_length(self, sample: List[float], tree: Dict, depth: int = 0) -> float:
        """Calculate path length for a sample through a tree"""
        if tree['type'] == 'leaf':
            # Estimate path length for leaf node
            size = tree['size']
            if size <= 1:
                return depth
            # Average path length of unsuccessful search in BST
            return depth + 2.0 * (math.log(size - 1) + 0.5772156649) - 2.0 * (size - 1) / size
        
        feature = tree['feature']
        split_value = tree['split_value']
        
        if sample[feature] < split_value:
            return self._path_length(sample, tree['left'], depth + 1)
        else:
            return self._path_length(sample, tree['right'], depth + 1)
    
    def _anomaly_score(self, sample: List[float]) -> float:
        """Calculate anomaly score for a sample"""
        if not self.trees:
            return 0.5
        
        # Average path length across all trees
        path_lengths = []
        for tree in self.trees:
            length = self._path_length(sample, tree)
            path_lengths.append(length)
        
        avg_path_length = sum(path_lengths) / len(path_lengths) if path_lengths else 0.0
        
        # Convert to anomaly score (higher = more anomalous)
        # Normalize based on expected path length
        c = 2.0 * (math.log(self.max_samples - 1) + 0.5772156649) - 2.0 * (self.max_samples - 1) / self.max_samples
        score = 2.0 ** (-avg_path_length / c)
        
        return score


class OneClassSVMSimple:
    """Simplified One-Class SVM implementation"""
    
    def __init__(self, nu: float = 0.1, gamma: float = 0.1):
        self.nu = nu
        self.gamma = gamma
        self.support_vectors = []
        self.center = []
        self.radius = 0.0
        self.fitted = False
    
    def fit(self, X: List[List[float]]) -> None:
        """Train one-class SVM (simplified as hypersphere)"""
        if not X or not X[0]:
            return
        
        n_features = len(X[0]) if X else 0
        
        # Calculate center (mean)
        if n_features > 0:
            self.center = [sum(row[i] for row in X) / len(X) for i in range(n_features)]
        else:
            self.center = []
        
        # Calculate distances from center
        distances = []
        for sample in X:
            dist = math.sqrt(sum((sample[i] - self.center[i]) ** 2 for i in range(len(sample))))
            distances.append(dist)
        
        # Set radius based on nu parameter (outlier fraction)
        distances_sorted = sorted(distances)
        percentile_idx = int(len(distances_sorted) * (1 - self.nu))
        self.radius = distances_sorted[percentile_idx] if percentile_idx < len(distances_sorted) else distances_sorted[-1]
        
        # Store some support vectors (samples near boundary)
        boundary_threshold = self.radius * 0.8
        self.support_vectors = []
        for i, dist in enumerate(distances):
            if dist >= boundary_threshold:
                self.support_vectors.append(X[i])
        
        self.fitted = True
    
    def predict(self, X: List[float]) -> Tuple[float, bool]:
        """Predict if sample is within the normal region"""
        if not self.fitted:
            return 0.5, False
        
        # Calculate distance from center
        distance = math.sqrt(sum((X[i] - self.center[i]) ** 2 for i in range(len(X))))
        
        # Convert to anomaly probability
        if self.radius == 0:
            probability = 0.5
        else:
            # Probability increases as distance from center increases
            probability = min(distance / (self.radius * 2), 1.0)
        
        is_anomalous = distance > self.radius
        return probability, is_anomalous


class BehavioralMLAnalyzer:
    """Main ML analyzer class"""
    
    def __init__(self):
        self.isolation_forest = IsolationForestSimple(n_estimators=50, contamination=0.15)
        self.one_class_svm = OneClassSVMSimple(nu=0.15)
        self.baseline_stats = {}
        self.fitted = False
    
    def fit_baseline(self, baseline_data: List[List[float]]) -> Dict[str, float]:
        """Train ML models on baseline behavioral data"""
        if not baseline_data or len(baseline_data) < 10:
            return {"success": False, "error": "Insufficient baseline data"}
        
        try:
            # Train models
            self.isolation_forest.fit(baseline_data)
            self.one_class_svm.fit(baseline_data)
            
            # Calculate baseline statistics
            n_features = len(baseline_data[0]) if baseline_data else 0
            n_samples = len(baseline_data)
            
            # Calculate mean
            means = [sum(row[i] for row in baseline_data) / n_samples for i in range(n_features)]
            
            # Calculate standard deviation
            stds = []
            for i in range(n_features):
                variance = sum((row[i] - means[i]) ** 2 for row in baseline_data) / n_samples
                stds.append(math.sqrt(variance))
            
            self.baseline_stats = {
                "mean": means,
                "std": stds,
                "n_samples": n_samples,
                "n_features": n_features
            }
            
            self.fitted = True
            
            return {
                "success": True,
                "n_samples": len(baseline_data),
                "n_features": len(baseline_data[0]),
                "models_trained": 2
            }
            
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    def analyze_sample(self, features: List[float]) -> Dict[str, float]:
        """Analyze a single behavioral sample"""
        if not self.fitted:
            return {
                "risk_score": 50.0,
                "confidence": 0.3,
                "isolation_score": 0.5,
                "svm_score": 0.5,
                "statistical_score": 0.5,
                "error": "Models not trained"
            }
        
        try:
            # Isolation Forest analysis
            iso_score, iso_anomalous = self.isolation_forest.predict(features)
            
            # One-Class SVM analysis
            svm_score, svm_anomalous = self.one_class_svm.predict(features)
            
            # Statistical analysis (Z-score based)
            stat_score = self._calculate_statistical_anomaly(features)
            
            # Ensemble scoring
            ensemble_score = (iso_score * 0.4 + svm_score * 0.4 + stat_score * 0.2)
            
            # Calculate confidence based on model agreement
            scores = [iso_score, svm_score, stat_score]
            confidence = self._calculate_confidence(scores)
            
            # Convert to risk percentage
            risk_score = ensemble_score * 100.0
            
            return {
                "risk_score": min(max(risk_score, 0.0), 100.0),
                "confidence": confidence,
                "isolation_score": iso_score,
                "svm_score": svm_score,
                "statistical_score": stat_score,
                "ensemble_agreement": len([s for s in scores if abs(s - ensemble_score) < 0.2]) / len(scores)
            }
            
        except Exception as e:
            return {
                "risk_score": 50.0,
                "confidence": 0.3,
                "error": str(e)
            }
    
    def _calculate_statistical_anomaly(self, features: List[float]) -> float:
        """Calculate statistical anomaly score using Z-scores"""
        if not self.baseline_stats:
            return 0.5
        
        mean = self.baseline_stats.get("mean", [0] * len(features))
        std = self.baseline_stats.get("std", [1] * len(features))
        
        z_scores = []
        for i, feature in enumerate(features):
            if i < len(mean) and i < len(std) and std[i] > 0:
                z_score = abs((feature - mean[i]) / std[i])
                z_scores.append(z_score)
        
        if not z_scores:
            return 0.5
        
        # Average Z-score converted to probability
        avg_z_score = sum(z_scores) / len(z_scores) if z_scores else 0.0
        probability = 1.0 - (1.0 / (1.0 + avg_z_score))
        return min(max(probability, 0.0), 1.0)
    
    def _calculate_confidence(self, scores: List[float]) -> float:
        """Calculate confidence based on model agreement"""
        if len(scores) < 2:
            return 0.5
        
        # Calculate variance among scores
        if len(scores) < 2:
            variance = 0.0
        else:
            mean = sum(scores) / len(scores)
            variance = sum((s - mean) ** 2 for s in scores) / len(scores)
        
        # Lower variance = higher confidence
        confidence = 1.0 / (1.0 + variance * 10.0)
        return min(max(confidence, 0.3), 0.95)
    
    def get_model_info(self) -> Dict[str, any]:
        """Get information about trained models"""
        return {
            "fitted": self.fitted,
            "baseline_samples": self.baseline_stats.get("n_samples", 0),
            "n_features": self.baseline_stats.get("n_features", 0),
            "models": [
                {"name": "Isolation Forest", "n_estimators": self.isolation_forest.n_estimators},
                {"name": "One-Class SVM", "nu": self.one_class_svm.nu}
            ]
        }


# Global analyzer instance
_analyzer = BehavioralMLAnalyzer()


def train_baseline(baseline_data_json: str) -> str:
    """Train ML models on baseline data (called from Kotlin)"""
    try:
        baseline_data = json.loads(baseline_data_json)
        result = _analyzer.fit_baseline(baseline_data)
        return json.dumps(result)
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def analyze_behavior(features_json: str) -> str:
    """Analyze behavioral features (called from Kotlin)"""
    try:
        features = json.loads(features_json)
        result = _analyzer.analyze_sample(features)
        return json.dumps(result)
    except Exception as e:
        return json.dumps({"risk_score": 50.0, "confidence": 0.3, "error": str(e)})


def get_model_status() -> str:
    """Get model training status (called from Kotlin)"""
    try:
        info = _analyzer.get_model_info()
        return json.dumps(info)
    except Exception as e:
        return json.dumps({"fitted": False, "error": str(e)})